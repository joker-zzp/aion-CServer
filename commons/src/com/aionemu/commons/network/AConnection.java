package com.aionemu.commons.network;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.network.packet.BaseServerPacket;
import com.aionemu.commons.options.Assertion;


/**
 * Class that represent Connection with server socket. Connection is created by <code>ConnectionFactory</code> and attached to
 * <code>SelectionKey</code> key. Selection key is registered to one of Dispatchers <code>Selector</code> to handle io read and write.
 * 
 * @author -Nemesiss-
 */
public abstract class AConnection<T extends BaseServerPacket> {

  /**
   * SocketChannel representing this connection
   */
  private final SocketChannel socketChannel;
  /**
   * Dispatcher [AcceptReadWriteDispatcherImpl] to which this connection SelectionKey is registered.
   */
  private final Dispatcher dispatcher;
  /**
   * SelectionKey representing this connection.
   */
  private SelectionKey key;
  /**
   * Time when the closing connection should be force-closed
   */
  protected long pendingCloseUntilMillis;
  /**
   * True if this connection is already closed.
   */
  protected boolean closed;
  /**
   * Object on which some methods are synchronized
   */
  protected final Object guard = new Object();
  /**
   * ByteBuffer for io write.
   */
  public final ByteBuffer writeBuffer;
  /**
   * ByteBuffer for io read.
   */
  public final ByteBuffer readBuffer;

  /**
   * Caching ip address to make sure that {@link #getIP()} method works even after disconnection
   */
  private final String ip;

  /**
   * Used only for PacketProcessor synchronization purpose
   */
  private boolean locked = false;

  /**
   * Constructor
   * 
   * @param sc
   * @param d
   * @throws IOException
   */
  private static final Logger log = LoggerFactory.getLogger(AConnection.class);
  
  public AConnection(SocketChannel sc, Dispatcher d, int rbSize, int wbSize) throws IOException {
    socketChannel = sc;
    dispatcher = d;
    writeBuffer = ByteBuffer.allocate(wbSize);
    writeBuffer.flip();
    writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
    readBuffer = ByteBuffer.allocate(rbSize);
    readBuffer.order(ByteOrder.LITTLE_ENDIAN);

    this.ip = socketChannel.socket().getInetAddress().getHostAddress();
    
    // 添加IP映射逻辑，解决Docker网络环境下的IP差异问题
    // 使用反射访问NetworkConfig，避免模块间循环依赖
    boolean enableIpMapping = false;
    try {
      Class<?> networkConfigClass = Class.forName("com.aionemu.gameserver.configs.network.NetworkConfig");
      java.lang.reflect.Field enableIpMappingField = networkConfigClass.getField("ENABLE_IP_MAPPING");
      enableIpMapping = enableIpMappingField.getBoolean(null);
    } catch (Exception e) {
      log.debug("Failed to check IP mapping configuration: {}", e.getMessage());
    }
    
    if (enableIpMapping) {
      try {
        InetSocketAddress remoteAddress = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();
        
        if (remoteAddress != null) {
          String remoteIp = remoteAddress.getAddress().getHostAddress();
          
          // 获取配置文件中设置的服务器连接地址
          String configuredServerIp = "0.0.0.0";
          try {
            Class<?> networkConfigClass = Class.forName("com.aionemu.gameserver.configs.network.NetworkConfig");
            java.lang.reflect.Field clientConnectAddressField = networkConfigClass.getField("CLIENT_CONNECT_ADDRESS");
            InetSocketAddress clientConnectAddress = (InetSocketAddress) clientConnectAddressField.get(null);
            configuredServerIp = clientConnectAddress.getAddress().getHostAddress();
          } catch (Exception e) {
            log.debug("Failed to get configured server IP: {}", e.getMessage());
          }
          
          // 如果客户端连接的是配置的服务器地址，或者在Docker环境中
          boolean isDockerEnvironment = false;
          
          // 检查是否在Docker环境中(本地地址是内部地址，远程地址是公网地址)
          InetAddress localAddress = socketChannel.socket().getLocalAddress();
          if (localAddress != null) {
            String localIp = localAddress.getHostAddress();
            isDockerEnvironment = (localIp.equals("127.0.0.1") || localIp.startsWith("172.17.") || localIp.startsWith("172.18.") || 
                                  localIp.startsWith("192.168.") || localIp.startsWith("10.")) && 
                                 (!remoteIp.equals("127.0.0.1") && !remoteIp.startsWith("172.17.") && !remoteIp.startsWith("172.18.") && 
                                  !remoteIp.startsWith("192.168.") && !remoteIp.startsWith("10."));
          }
          
          // 如果是Docker环境或者客户端直接连接到配置的服务器IP
          if (isDockerEnvironment || remoteIp.equals(configuredServerIp)) {
            
            // 使用反射调用World.addIpMapping方法，避免循环依赖
            try {
              Class<?> worldClass = Class.forName("com.aionemu.gameserver.world.World");
              Method getInstanceMethod = worldClass.getMethod("getInstance");
              Object worldInstance = getInstanceMethod.invoke(null);
              Method addIpMappingMethod = worldClass.getMethod("addIpMapping", String.class, String.class);
              
              // 添加公网IP到内部IP的映射
              addIpMappingMethod.invoke(worldInstance, remoteIp, this.ip);
              log.debug("Added IP mapping for connection: public={}, internal={}", remoteIp, this.ip);
            } catch (Exception e) {
              log.warn("Failed to add IP mapping: {}", e.getMessage());
            }
          }
        }
      } catch (Exception e) {
        log.warn("Error during IP mapping setup: {}", e.getMessage());
      }
    }
  }

  /**
   * Set selection key - result of registration this AConnection socketChannel to one of dispatchers.
   * 
   * @param key
   */
  final void setKey(SelectionKey key) {
    this.key = key;
  }

  /**
   * @return SocketChannel representing this connection.
   */
  public SocketChannel getSocketChannel() {
    return socketChannel;
  }

  /**
   * Sends the ServerPacket to this client.
   */
  public final void sendPacket(T serverPacket) {
    synchronized (guard) {
      if (pendingCloseUntilMillis != 0 || closed)
        return;

      if (isConnected()) {
        getSendMsgQueue().add(serverPacket);
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        key.selector().wakeup();
      } else {
        close();
      }
    }
  }

  /**
   * Connection will be closed at some time [by Dispatcher Thread], after that onDisconnect() method will be called to clear all other things.
   */
  public final void close() {
    close(null);
  }

  /**
   * Its guaranteed that closePacket will be sent before closing connection, but all past and future packets wont. Connection will be closed [by
   * Dispatcher Thread], and onDisconnect() method will be called to clear all other things.
   * 
   * @param closePacket
   *          Packet that will be sent before closing. If closePacket is null, regular {@link #close()} will be called instead.
   */
  public final void close(T closePacket) {
    synchronized (guard) {
      if (pendingCloseUntilMillis != 0 || closed)
        return;

      pendingCloseUntilMillis = System.currentTimeMillis() + 2000;
      if (closePacket != null || !isConnected())
        getSendMsgQueue().clear();
      if (closePacket != null && isConnected()) {
        getSendMsgQueue().add(closePacket);
        key.interestOps(SelectionKey.OP_WRITE);
      }
      dispatcher.closeConnection(this);
      key.selector().wakeup(); // notify dispatcher
    }
  }

  protected abstract Queue<T> getSendMsgQueue();

  /**
   * This will close the connection and call onDisconnect() on another thread. May be called only by Dispatcher Thread.
   */
  final void disconnect(Executor dcExecutor) {
    // Test if this build should use assertion. If NetworkAssertion == false javac will remove this code block
    if (Assertion.NetworkAssertion)
      assert Thread.currentThread() == dispatcher;

    synchronized (guard) {
      if (closed)
        return;
      closed = true;
    }

    key.cancel();
    try {
      socketChannel.close();
    } catch (IOException ignored) {
    }
    key.attach(null);

    dcExecutor.execute(this::onDisconnect);
  }

  final boolean isConnected() {
    return key.isValid();
  }

  /**
   * @return True if this connection is pendingClose and not closed yet.
   */
  final boolean isPendingClose() {
    return pendingCloseUntilMillis != 0 && !closed;
  }

  final boolean isClosed() {
    return closed;
  }

  /**
   * @return IP address of this Connection.
   */
  public final String getIP() {
    return ip;
  }

  /**
   * Used only for PacketProcessor synchronization purpose. Return true if locked successful - if wasn't locked before.
   * 
   * @return locked
   */
  boolean tryLockConnection() {
    if (locked)
      return false;
    return locked = true;
  }

  /**
   * Used only for PacketProcessor synchronization purpose. Unlock this connection.
   */
  void unlockConnection() {
    locked = false;
  }

  /**
   * @param data
   * @return True if data was processed correctly, False if some error occurred and connection should be closed NOW.
   */
  protected abstract boolean processData(ByteBuffer data);

  /**
   * This method will be called by Dispatcher, and will be repeated till return false.
   * 
   * @param data
   * @return True if data was written to buffer, False indicating that there are not any more data to write.
   */
  protected abstract boolean writeData(ByteBuffer data);

  /**
   * Called when AConnection object is fully initialized and ready to process and send packets. It may be used as hook for sending first packet etc.
   */
  protected abstract void initialized();

  /**
   * This method is called to inform that this connection was closed and should be cleared. This method is called only once.
   */
  protected abstract void onDisconnect();

  /**
   * This method is called by NioServer to inform that NioServer is shouting down. This method is called only once.
   */
  protected abstract void onServerClose();
}