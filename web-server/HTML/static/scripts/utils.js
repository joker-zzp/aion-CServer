// 表格模板案例
const tableTemplate = {
  title: '技能列表',
  body: [
    {key: 'id', label: '技能ID', show: true, format: (item) => item.id || '-'},
    {key: 'name', label: '技能名称', show: true, format: (item) => item.name || '-'},
    {key: 'name_id', label: '技能名称ID', show: true, format: (item) => item.name_id || '-'},
  ]
}

/** 渲染表格函数
 * @param {*} container 表格容器元素
 * @param {*} template 表格模板配置
 * @returns {void}
 */
function renderTable(container, template) {
  // 清空表格
  container.innerHTML = '';
	// 渲染表格
	container.innerHTML = `
		<table class="${template.class || 'results-table'}">
			<thead></thead>
			<tbody></tbody>
		</table>
	`;
}

/** 渲染表头函数
 * @param {*} container 表格容器元素
 * @param {*} template 表格模板配置
 * @returns {void}
 */
function renderTableHeader(container, template) {
    // 清空表格 在元素找到 thead 元素
    const thead = container.querySelector('thead');
		if (!thead) {
			return;
		}
    // 清空表头
    thead.innerHTML = '';
    // 渲染表头
    thead.innerHTML = `<tr>${template.body.filter((item) => item.show).map((item) => `<th>${item.label}</th>`).join('')}</tr>`;
}

/**
 * 渲染表格数据函数
 * @param {*} container 表格容器元素
 * @param {*} template 表格模板配置
 * @param {*} data 表格数据数组
 * @returns {void}
 */
function renderTableData(container, template, data) {
    // 清空表格 在元素找到 tbody 元素
    const tbody = container.querySelector('tbody');
    // 清空表格数据
    tbody.innerHTML = '';
    // 渲染表格数据
    tbody.innerHTML = data.map((item) => `
        <tr>
            ${template.body
              .filter((t) => t.show)
              .map((field) => `
                <td>${field.format(item)}</td>
              `)
              .join('')}
        </tr>
    `).join('');
}

// 分页模板 案例
const pageTemplate = {
	show: true,
	class: 'pagination',
	pagebut: {
		prev: {
			class: 'page-btn',
			text: '上一页',
			disabled: (page, total) => page <= 1,
		},
		next: {
			class: 'page-btn',
			text: '下一页',
			disabled: (page, total) => page >= total,
		}
	},
	// 每页大小数量选择器
	pageSize: {
		show: true,
		class: 'page-size',
		options: [
			{value: 10, text: '10条/页'},
			{value: 20, text: '20条/页'},
			{value: 30, text: '30条/页'},
			{value: 40, text: '40条/页'},
		]
	},
	// 分页信息
	pageInfo: {
		show: true,
		class: 'page-info',
		text: '第 {{currentPage}} 页，共 {{totalPages}} 页',
	}
}

/** 渲染分页函数
 * @param {*} container 分页容器元素
 * @param {*} template 分页模板配置
 * @param {*} data 分页数据 {currentPage, totalPages, pageSize}
 * @param {*} onPageChange 页码变化时的回调函数，接收新页码作为参数
 * @returns {void}
 */
function renderPagination(container, template, data = {}, onPageChange = null) {
  // 清空分页
  container.innerHTML = '';

	if (template.class) {
		container.className = template.class;
	}
	// 是否显示分页
	if (!template.show) {
		return;
	}
  
  // 创建上一页按钮
  const prevButton = document.createElement('button');
	if (template.pagebut.prev.class) {
		prevButton.className = template.pagebut.prev.class;
	}
  prevButton.textContent = template.pagebut.prev.text || 'Prev';
  prevButton.dataset.page = data.currentPage - 1;
  
  // 根据条件设置上一页按钮禁用状态
  if (template.pagebut.prev.disabled(data.currentPage)) {
    prevButton.disabled = true;
    prevButton.classList.add('disabled');
  }
  
  // 添加上一页按钮点击事件
  if (onPageChange && !prevButton.disabled) {
    prevButton.addEventListener('click', function() {
      onPageChange(parseInt(this.dataset.page));
    });
  }
  
  // 创建下一页按钮
  const nextButton = document.createElement('button');
	if (template.pagebut.next.class) {
		nextButton.className = template.pagebut.next.class;
	}
  nextButton.textContent = template.pagebut.next.text || 'Next';
  nextButton.dataset.page = data.currentPage + 1;
  
  // 根据条件设置下一页按钮禁用状态
  if (template.pagebut.next.disabled(data.currentPage, data.totalPages)) {
    nextButton.disabled = true;
    nextButton.classList.add('disabled');
  }
  
  // 添加下一页按钮点击事件
  if (onPageChange && !nextButton.disabled) {
    nextButton.addEventListener('click', function() {
      onPageChange(parseInt(this.dataset.page));
    });
  }
  
  // 创建分页信息
  let pageInfoElement = null;
  if (template.pageInfo.show) {
    pageInfoElement = document.createElement('span');
    if (template.pageInfo.class) {
      pageInfoElement.className = template.pageInfo.class;
    }
    // 替换分页信息文本中的变量
    pageInfoElement.textContent = template.pageInfo.text
      .replace('{{currentPage}}', data.currentPage)
      .replace('{{totalPages}}', data.totalPages);
  }
  
  // 创建分页大小选择器
  let pageSizeElement = null;
  if (template.pageSize.show) {
    pageSizeElement = document.createElement('select');
    if (template.pageSize.class) {
      pageSizeElement.className = template.pageSize.class;
    }

    // 添加分页大小选项
    template.pageSize.options.forEach(option => {
      const opt = document.createElement('option');
      opt.value = option.value;
      opt.textContent = option.text;
      if (option.value === data.pageSize) {
        opt.selected = true;
      }
      pageSizeElement.appendChild(opt);
    });
  }
  
  // 添加所有元素到容器
  container.appendChild(prevButton);
  if (pageInfoElement) {
    container.appendChild(pageInfoElement);
  }
  container.appendChild(nextButton);
  if (pageSizeElement) {
    container.appendChild(pageSizeElement);
  }
}

/** 渲染 loading
 * @param {*} container 容器元素
 * @param {*} collback 回调函数
 * @param {*} time 延迟时间
 */
function renderLoading(container, collback, time = 1000) {
  container.innerHTML = '<div class="placeholder">加载中...</div>';
  setTimeout(() => {
    collback();
  }, time);
}

/** Api 请求工具函数
 * @param {*} url 请求地址
 * @param {*} method 请求方法
 * @param {*} params 请求参数 {key: value}
 * @param {*} data 请求数据 {key: value}
 * @returns {Promise} 请求结果
 */
async function apiRequest(url, method = 'GET', params = {}, data = null) {
  const options = { method };

	//  GET 请求需要添加查询参数 解析 params 对象转换成 请求参数
	if (method.toUpperCase() === 'GET' && Object.keys(params).length > 0) {
    url += `?${new URLSearchParams(params).toString()}`;
  }
	//  POST 请求需要添加请求体 解析 data 对象转换成 请求体
  if (data) {
		options.headers = { 'Content-Type': 'application/json' };
    options.body = JSON.stringify(data);
  }

  try {
    const response = await fetch(url, options);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    // 尝试解析 JSON，但提供备选方案
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return await response.json();
    } else {
      return await response.text();
    }
  } catch (error) {
    console.error('API request failed:', error);
    throw error;
  }
}

/** 防抖 */
function debounce(func, delay) {
  let timeoutId;
  return function(...args) {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => func.apply(this, args), delay);
  };
}

export default {
	apiRequest,
	debounce,
  renderTable,
	renderPagination,
  renderTableHeader,
  renderTableData,
  renderLoading,
}