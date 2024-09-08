(function(){

	var pageElemQuery = 'div.main-wrapper[data-docusaurus-static-path]';
	var documentData = document.documentElement.dataset;

	function ToggleElemDisplay (elem) {
		elem.style.display = (!elem.style.display ? 'revert' : '');
	}

	/* remove duplicate menu buttons */
	Array.from(document.querySelectorAll('div.navbar__items > button.navbar__toggle')).slice(1).forEach(function(elem){
		elem.remove();
	});

	/* initialize navigation menu */
	document.querySelector('div.navbar__items > button.navbar__toggle').onclick = function(){
		Array.from(document.querySelectorAll('div[class*="docRoot_"] > aside.theme-doc-sidebar-container, div.main-wrapper nav[class*="sidebar_"]')).forEach(function(elem){
			ToggleElemDisplay(elem);
		});
	};

	/* theme switching */
	var themeButtonElem = document.querySelector('div[class*="colorModeToggle_"] > button');
	themeButtonElem.disabled = false;
	themeButtonElem.onclick= function(){
		documentData.theme = (!documentData.theme || documentData.theme === 'light' ? 'dark' : 'light');
		localStorage.setItem('theme', documentData.theme);
		var lightStyle = document.querySelector(`div.navbar__logo > img[class*="light"]`).style;
		var darkStyle = document.querySelector(`div.navbar__logo > img[class*="dark"]`).style;
		switch (documentData.theme) {
			case 'light':
				lightStyle.display = 'unset';
				darkStyle.display = 'none';
			break;
			case 'dark':
				lightStyle.display = 'none';
				darkStyle.display = 'unset';
			break;
		}
	};

	/* set document to default theme */
	if (!['light', 'dark'].includes(documentData.theme)) {
		documentData.theme = 'light';
	}

	window.addEventListener('load', function(){

		/* initialize ToC menus */
		var desktopTocQuery = 'div.theme-doc-toc-desktop';
		var mobileTocQuery = 'div.theme-doc-toc-mobile';
		Array.from(document.querySelectorAll('div[class*="docRoot_"]')).forEach(function(docElem){
			var mobileTocElem = docElem.querySelector(`${mobileTocQuery}`);
			if (mobileTocElem) {
				mobileTocElem.innerHTML += docElem.querySelector(`${desktopTocQuery}`).outerHTML;
				var newTocElem = mobileTocElem.querySelector(`${desktopTocQuery}`);
				var newTocListElem = newTocElem.querySelector('ul');
				newTocListElem.className = newTocListElem.className.replaceAll('table-of-contents__left-border', '');
				docElem.querySelector(`${mobileTocQuery} > button`).onclick= function(){
					ToggleElemDisplay(newTocElem);
				};
			}
		});

	});

})();
