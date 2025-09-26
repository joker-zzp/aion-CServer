import utils from './utils.js';

const itemTypeDict = {
	"DAGGER": "短剑",
	"SWORD": "长剑",
	"GREATSWORD": "巨剑",
	"POLEARM": "战戟",
	"STAFF": "法杖",
	"BOW": "弓",
	"GUN": "枪械",
	"CANNON": "火炮",
	"HARP": "琴",
	"KEYBLADE": "钥匙刀",
	"MACE": "锤",
	"ORB": "宝珠",
	"SPELLBOOK": "法术书",
	"RB_TORSO": "上衣-布甲",
	"RB_GLOVE": "手套-布甲",
	"RB_SHOULDER": "肩膀-布甲",
	"RB_PANTS": "裤子-布甲",
	"RB_SHOES": "鞋子-布甲",
	"LT_TORSO": "上衣-皮甲",
	"LT_GLOVE": "手套-皮甲",
	"LT_SHOULDER": "肩膀-皮甲",
	"LT_PANTS": "裤子-皮甲",
	"LT_SHOES": "鞋子-皮甲",
	"CH_TORSO": "上衣-链甲",
	"CH_GLOVE": "手套-链甲",
	"CH_SHOULDER": "肩膀-链甲",
	"CH_PANTS": "裤子-链甲",
	"CH_SHOES": "鞋子-链甲",
	"PL_TORSO": "上衣-金属",
	"PL_GLOVE": "手套-金属",
	"PL_SHOULDER": "肩膀-金属",
	"PL_PANTS": "裤子-金属",
	"PL_SHOES": "鞋子-金属",
	"HEAD": "头盔",
	"SHIELD": "盾",
	"EARRING": "耳环",
	"NECKLACE": "项链",
	"RING": "戒指",
	"BELT": "腰带",
	"RECIPE": "图纸",
}
function itemType(value) {
	if (Object.keys(itemTypeDict).includes(value)) {
		return itemTypeDict[value];
	}
	return value;
}

document.addEventListener('DOMContentLoaded', function() {
    console.log('Items.js脚本已加载并执行');
    
    // 获取DOM元素
    const searchInput = document.getElementById('item-search');
    const searchBtn = document.getElementById('search-btn');
    const resultsContainer = document.getElementById('search-results');
    const paginationContainer = document.querySelector('.pagination');

    // 表格模板 - 与skills.js保持一致的格式
    const tableTemplate = {
      title: '物品列表',
      class: 'results-table',
      body: [
        {key: 'id', label: '物品ID', show: true, format: (item) => item.id || '-'},
        {key: 'name', label: '物品名称', show: true, format: (item) => item.name || '-'},
        {key: 'level', label: '使用等级', show: true, format: (item) => item.level || '-'},
				{key: 'item_group', label: '物品类型', show: true, format: (item) => itemType(item.item_group) || '-'},
      ]
    }

    // 分页模板 - 与skills.js保持一致的格式
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
        console.log('搜索按钮被点击');
        const query = searchInput.value.trim();
        if (query) {
            currentQuery = query;
            currentPage = 1;
            debouncedSearchItems(query, currentPage);
        }
    });

    // 回车键搜索
    searchInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            console.log('回车键被按下');
            const query = searchInput.value.trim();
            if (query) {
                currentQuery = query;
                currentPage = 1;
                debouncedSearchItems(query, currentPage);
            }
        }
    });

    // 搜索物品函数 - 使用后端API和utils工具函数
    function searchItems(query, page) {
        console.log(`搜索物品: ${query}, 第 ${page} 页`);
        
        // 创建请求参数
        const params = {
          q: query,
          page: page,
          size: pageSize,
        }

        // 使用utils.apiRequest工具函数
        utils.apiRequest('/api/items/search', 'GET', params)
        .then(res => {
          if (res.code === 0 && Array.isArray(res.data)) {
            const data = res.data;
            // 使用utils工具函数渲染表格数据
            utils.renderTableData(resultsContainer, tableTemplate, data);
            // 使用utils工具函数渲染分页
            utils.renderPagination(paginationContainer, paginationTemplate, {
              currentPage: res.page || page,
              pageSize: res.size || pageSize,
              totalPages: Math.ceil(res.total / (res.size || pageSize)),
            }, (pageNum) => {
              debouncedSearchItems(currentQuery, pageNum);
            });
          }
        })
        .catch(error => {
          console.error('搜索物品时出错:', error);
          resultsContainer.innerHTML = `<div class="placeholder">${error.message}</div>`;
        });
    }

    // 使用utils.debounce工具函数添加防抖功能
    const debouncedSearchItems = utils.debounce(searchItems, 300);

    // 页面初始化
    function initPage() {
        console.log('页面初始化开始');
        
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
        
        // 执行空搜索，获取第一页的物品数据
        setTimeout(() => {
            searchItems('', 1);
        }, 100);
    }

    // 启动页面初始化
    initPage();

    console.log('页面初始化完成');
});