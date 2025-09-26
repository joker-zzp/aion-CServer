import utils from './utils.js';

document.addEventListener('DOMContentLoaded', () => {
  
  // 获取DOM 元素
  const searchInput = document.getElementById('npc-search');
  const searchBtn = document.getElementById('search-btn');
  const resultsContainer = document.getElementById('search-results');

  // 分页容器
  const paginationContainer = document.querySelector('.pagination');

  // 表格模板
  const tableTemplate = {
    title: 'NPC列表',
    class: 'results-table',
    body: [
      {key: 'id', label: 'ID', show: true, format: (item) => item.id || '-'},
      {key: 'name', label: '名称', show: true, format: (item) => item.name || '-'},
			{key: 'title', label: '称号', show: true, format: (item) => item.title || '-'},
			{key: 'level', label: '等级', show: true, format: (item) => item.level || '-'},
    ]
  };

  // 分页模板
  const paginationTemplate = {
    show: true,
    pagebut: {
      prev: {
        text: '上一页',
        disabled: (page) => page <= 1,
      },
      next: {
        text: '下一页',
        disabled: (page, total) => page >= total,
      },
    },
    pageSize: {
      show: true,
      class: 'page-size',
      options: [
        {value: 10, text: '10条/页'},
        {value: 20, text: '20条/页'},
      ]
    },
    pageInfo: {
      show: true,
      class: 'page-info',
      text: '第 {{currentPage}} 页，共 {{totalPages}} 页',
    }
  }

  let currentQuery = '';
  let currentPage = 1;
  let totalPages = 1;
  let pageSize = 10;


  
  // 请求查询
  function queryNpc(query, page) {
    utils.apiRequest('/api/npcs/query', 'GET', {
      q: query,
      page: page,
      size: pageSize,
    }).then(res => {
      if (res.code === 0 && Array.isArray(res.data)) {
				const data = res.data;
        // 渲染表格数据
				utils.renderTableData(resultsContainer, tableTemplate, data);
				// 渲染分页
        utils.renderPagination(paginationContainer, paginationTemplate, {
          currentPage: res.page,
          pageSize: res.size,
          totalPages: Math.ceil(res.total / res.size),
        }, (page) => debouncedSearchNpcs(currentQuery, page));
      }
    })
  }

	const debouncedSearchNpcs = utils.debounce(queryNpc, 300);

  function initPage() {
    console.log('开始初始化页面, 加载NPC列表');
    currentQuery = '';
    currentPage = 1;
    // 检查是否存在表格不存在先创建表格
    if (!resultsContainer.querySelector('table')) {
      utils.renderTable(resultsContainer, tableTemplate);
      // 渲染表头
      utils.renderTableHeader(resultsContainer, tableTemplate);
    }
    // 渲染分页
    utils.renderPagination(paginationContainer, paginationTemplate, {
      currentPage: currentPage,
      pageSize: pageSize,
      totalPages: totalPages,
    });
    // 执行空搜索，获取第一页的技能数据
    queryNpc('', 1);
  }

  // 初始化页面
  initPage();

		// 搜索按钮点击事件
	searchBtn.addEventListener('click', function() {
		const query = searchInput.value.trim();
		if (query) {
				currentQuery = query;
				currentPage = 1;
				debouncedSearchNpcs(query, currentPage);
		}
	});

	// 回车键搜索
	searchInput.addEventListener('keypress', function(e) {
			if (e.key === 'Enter') {
					const query = searchInput.value.trim();
					if (query) {
							currentQuery = query;
							currentPage = 1;
							debouncedSearchNpcs(query, currentPage);
					}
			}
	});
});