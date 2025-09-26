package admincommands;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.commons.configuration.Properties;
import com.aionemu.commons.configuration.TransformationException;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.utils.collections.Predicates;

/**
 * @author ATracer, Rolandas, Neon
 */
public class Configure extends AdminCommand {

	public Configure() {
		super("configure", "显示/修改配置设置。");

		// @formatter:off
		setSyntaxInfo(
			"<list> - 显示所有可用的配置类别.",
			"<category> - 显示指定配置的所有可用属性.",
			"<category> <property> - 显示属性的当前值",
			"<category> <property> <value> - 将属性值更改为新值."
		);
		// @formatter:on
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			sendInfo(admin);
			return;
		}

		Map<String, Class<?>> configs = Config.getClasses().stream()
					.sorted(Comparator.comparing(Class::getSimpleName, String.CASE_INSENSITIVE_ORDER))
					.collect(Collectors.toMap(cls -> cls.getSimpleName().toLowerCase().replace("config", ""), cls -> cls, (u, v) -> u, LinkedHashMap::new));
		if ("list".equalsIgnoreCase(params[0])) {
			StringBuilder sb = new StringBuilder("可用的配置名称列表:");
			for (String configname : configs.keySet())
				sb.append("\n\t").append(configname);
			sendInfo(admin, sb.toString());
		} else {
			Class<?> cls = configs.get(params[0].toLowerCase());
			if (cls == null) {
				sendInfo(admin, "无效的配置名称。您可以通过<list>参数获取可用配置类别的列表。");
				return;
			}
			if (params.length < 2) {
				StringBuilder sb = new StringBuilder("可用的属性列表: ").append(cls.getSimpleName()).append(":");
				for (Field field : findStaticFields(cls, Predicates.alwaysTrue())) {
					try {
						String value = getFieldValue(field);
						sb.append("\n\t").append(field.getName()).append("\t=\t").append(value);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						sb.append("\n\t").append(field.getName()).append("\t=\t").append("读取值错误: ").append(e.getMessage());
					}
				}
				sendInfo(admin, sb.toString());
				return;
			}
			String fieldName = params[1].toUpperCase();
			try {
				List<Field> fields = findStaticFields(cls, field -> field.getName().equals(fieldName));
				if (fields.isEmpty()) {
					sendInfo(admin, cls.getSimpleName() + "." + fieldName + " 不存在.");
					return;
				}
				Field field = fields.get(0);
				String value = getFieldValue(field);
				if (params.length > 2) {
					String newValue = StringUtils.join(params, ' ', 2, params.length);
					try {
						if (field.isAnnotationPresent(Properties.class))
							field.set(null, ConfigurableProcessor.transform(toMap(newValue), field));
						else
							field.set(null, ConfigurableProcessor.transform(newValue, field));
					} catch (TransformationException e) {
						sendInfo(admin, "无法设置新值: " + e.getCause().getMessage());
						return;
					}
					sendInfo(admin,
									cls.getSimpleName() + "." + fieldName + "的值已从" + value + "更改为" + getFieldValue(field));
				} else {
					sendInfo(admin, cls.getSimpleName() + "." + fieldName + "的当前值是" + value);
				}
			} catch (Exception e) {
				sendInfo(admin, "无法访问" + cls.getSimpleName() + "." + fieldName);
			}
		}
	}

	private Map<String, String> toMap(String value) {
		Map<String, String> values = new LinkedHashMap<>();
		if (value.startsWith("{") && value.endsWith("}"))
			value = value.substring(1, value.length() - 1);
		String[] entries = value.contains(",") ? value.split(" *, *") : value.split(" +");
		try {
			for (String entry : entries) {
				int indexOfEqualsSign = entry.indexOf('=');
				if (indexOfEqualsSign == -1)
					throw new IllegalArgumentException(entry + "后缺少值 (格式: key=value)");
				values.put(entry.substring(0, indexOfEqualsSign).trim(), entry.substring(indexOfEqualsSign + 1).trim());
			}
		} catch (Exception e) {
			throw new TransformationException(null, e);
		}
		return values;
	}

	private String getFieldValue(Field field) throws IllegalArgumentException, IllegalAccessException {
		Object value = field.get(null);
		if (value != null && value.getClass().isArray()) {
			if (value.getClass().getComponentType().isPrimitive()) {
				int length = Array.getLength(value);
				Object[] objArr = new Object[length];
				for (int i = 0; i < length; i++)
					objArr[i] = Array.get(value, i);
				value = Arrays.toString(objArr);
			} else
				value = Arrays.toString((Object[]) value);
		}
		return String.valueOf(value);
	}

	private static List<Field> findStaticFields(Class<?> cls, Predicate<Field> filter) {
		return Arrays.stream(cls.getDeclaredFields()).filter(filter.and(field -> Modifier.isStatic(field.getModifiers()))).toList();
	}
}