// 全局JavaScript功能

// 导航模板
const navTemplate = {
  title: 'Aion Game Data Web Service',
  navLinks: [
    { title: '主页', href: '/' },
    { title: '技能查询', href: '/skills' },
    { title: '物品查询', href: '/items' },
    { title: 'NPC查询', href: '/npcs' },
  ],
};

// 渲染导航
function renderNav(header_element) {
  if (!header_element) return;
	// 创建一个导航标签
	// 创建一个 nav 标签
	const nav = document.createElement('nav');
	// 创建一个 ul 列表
	const navList = document.createElement('ul');
	// 遍历导航链接
	navTemplate.navLinks.forEach(link => {
		// 创建一个 li 标签
		const li = document.createElement('li');
		li.innerHTML = `<a href="${link.href}">${link.title}</a>`;
		navList.appendChild(li);
	});
	// 添加到 nav 元素
	nav.appendChild(navList);
	// 添加到 header 元素
	header_element.appendChild(nav);
}

function readerTitle(header_element) {
	if (!header_element) return;
	// 创建一个 h1 标题
	const title = document.createElement('h1');
	title.textContent = navTemplate.title;
	header_element.appendChild(title);
}

document.addEventListener('DOMContentLoaded', function() {
  const header = document.querySelector('header');
	// 清空 header 元素的内容
	header.innerHTML = '';
	readerTitle(header);
	renderNav(header);
});
