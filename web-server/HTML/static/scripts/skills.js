// 导入 utils 模块
import utils from './utils.js';

document.addEventListener('DOMContentLoaded', function() {
    console.log('Skills.js脚本已加载并执行');
    
    // 获取DOM元素
    const searchInput = document.getElementById('skill-search');
    const searchBtn = document.getElementById('search-btn');
    const resultsContainer = document.getElementById('search-results');
    // 消息
    const messageBox = document.querySelector('.message-box');
    // 分页容器
    const paginationContainer = document.querySelector('.pagination');

    // 表格模板
    const tableTemplate = {
      title: '技能列表',
      class: 'results-table',
      body: [
        {key: 'id', label: '技能ID', show: true, format: (item) => item.id || '-'},
        {key: 'name', label: '技能名称', show: true, format: (item) => item.name || '-'},
        {key: 'name_id', label: '技能名称ID', show: true, format: (item) => item.name_id || '-'},
        {key: 'level', label: '技能等级', show: true, format: (item) => item.level || '-'},
				{key: 'skillsubtype', label: '技能类型', show: true, format: (item) => item.skillsubtype || '-'},
      ]
    }

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

    // 当前搜索的查询参数和分页信息
    let currentQuery = '';
    let currentPage = 1;
    let totalPages = 1;
    let pageSize = 10;

    console.log('DOM元素已成功获取');

    // 搜索按钮点击事件
    searchBtn.addEventListener('click', function() {
        const query = searchInput.value.trim();
        if (query) {
            currentQuery = query;
            currentPage = 1;
            debouncedSearchSkills(query, currentPage);
        }
    });

    // 回车键搜索
    searchInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            const query = searchInput.value.trim();
            if (query) {
                currentQuery = query;
                currentPage = 1;
                debouncedSearchSkills(query, currentPage);
            }
        }
    });

    // 搜索技能函数 - 使用后端API
    function searchSkills(query, page) {
        console.log(`搜索技能: ${query}, 第 ${page} 页`);
        // 创建请求参数
        const params = {
          q: query,
          page: page,
          size: pageSize,
        }

        // 调用 apiRequest 函数
        utils.apiRequest('/api/skills/search', 'GET', params)
        .then(res => {
          if (res.code === 0 && Array.isArray(res.data)) {
            const data = res.data
            // 渲染表格数据
            utils.renderTableData(resultsContainer, tableTemplate, data);
            // 渲染分页
            utils.renderPagination(paginationContainer, paginationTemplate, {
              currentPage: res.page,
              pageSize: res.size,
              totalPages: Math.ceil(res.total / res.size),
            }, (page) => {
              debouncedSearchSkills(currentQuery, page);
            });
          }
        })
        .catch(error => {
          console.error('搜索技能时出错:', error);
        });
    }

		const debouncedSearchSkills = utils.debounce(searchSkills, 300);

    // 初始化页面
    console.log('页面初始化完成');

    // 页面加载时自动执行一次空搜索，显示初始技能列表
    function initPage() {
        console.log('开始初始化页面，加载初始技能列表');
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
        searchSkills('', 1);
    }

		// 初始化页面
    initPage();
});