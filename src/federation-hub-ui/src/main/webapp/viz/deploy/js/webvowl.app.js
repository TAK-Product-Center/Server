/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
webvowl.app =
/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId])
/******/ 			return installedModules[moduleId].exports;
/******/
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			exports: {},
/******/ 			id: moduleId,
/******/ 			loaded: false
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.loaded = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(0);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ function(module, exports, __webpack_require__) {

	__webpack_require__(70);
	__webpack_require__(72);
	
	module.exports = __webpack_require__(73);


/***/ },
/* 1 */,
/* 2 */,
/* 3 */,
/* 4 */,
/* 5 */,
/* 6 */
/***/ function(module, exports) {

	module.exports = d3;

/***/ },
/* 7 */,
/* 8 */,
/* 9 */,
/* 10 */,
/* 11 */,
/* 12 */,
/* 13 */,
/* 14 */,
/* 15 */,
/* 16 */,
/* 17 */,
/* 18 */,
/* 19 */,
/* 20 */,
/* 21 */,
/* 22 */,
/* 23 */,
/* 24 */,
/* 25 */,
/* 26 */,
/* 27 */,
/* 28 */,
/* 29 */,
/* 30 */,
/* 31 */,
/* 32 */,
/* 33 */,
/* 34 */,
/* 35 */,
/* 36 */,
/* 37 */,
/* 38 */,
/* 39 */,
/* 40 */,
/* 41 */,
/* 42 */,
/* 43 */,
/* 44 */,
/* 45 */,
/* 46 */,
/* 47 */,
/* 48 */,
/* 49 */,
/* 50 */,
/* 51 */,
/* 52 */,
/* 53 */,
/* 54 */,
/* 55 */,
/* 56 */,
/* 57 */,
/* 58 */,
/* 59 */,
/* 60 */,
/* 61 */,
/* 62 */,
/* 63 */,
/* 64 */,
/* 65 */,
/* 66 */,
/* 67 */,
/* 68 */,
/* 69 */,
/* 70 */
/***/ function(module, exports) {

	// removed by extract-text-webpack-plugin

/***/ },
/* 71 */,
/* 72 */
/***/ function(module, exports) {

	/* Taken from here: http://stackoverflow.com/a/17907562 */
	function getInternetExplorerVersion() {
		var ua,
			re,
			rv = -1;
		if (navigator.appName === "Microsoft Internet Explorer") {
			ua = navigator.userAgent;
			re = new RegExp("MSIE ([0-9]{1,}[\\.0-9]{0,})");
			if (re.exec(ua) !== null) {
				rv = parseFloat(RegExp.$1);
			}
		} else if (navigator.appName === "Netscape") {
			ua = navigator.userAgent;
			re = new RegExp("Trident/.*rv:([0-9]{1,}[\\.0-9]{0,})");
			if (re.exec(ua) !== null) {
				rv = parseFloat(RegExp.$1);
			}
		}
		return rv;
	}
	
	function showBrowserWarningIfRequired() {
		var version = getInternetExplorerVersion();
		if (version > 0 && version <= 11) {
			document.write("<div id=\"browserCheck\">The WebVOWL demo does not work in Internet Explorer. Please use another browser, such as <a href=\"http://www.mozilla.org/firefox/\">Mozilla Firefox</a> or <a href=\"https://www.google.com/chrome/\">Google Chrome</a>, to run the WebVOWL demo.</div>");
			// hiding any additional menus and features
			var canvasArea = document.getElementById("canvasArea"),
				detailsArea = document.getElementById("detailsArea"),
				optionsArea = document.getElementById("optionsArea");
			canvasArea.className = "hidden";
			detailsArea.className = "hidden";
			optionsArea.className = "hidden";
		}
	}
	
	
	module.exports = showBrowserWarningIfRequired;


/***/ },
/* 73 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {module.exports = function () {
	
		var app = {},
			graph = webvowl.graph(),
			options = graph.graphOptions(),
			languageTools = webvowl.util.languageTools(),
			graphSelector = "#graph",
		// Modules for the webvowl app
			ontologyMenu,
			exportMenu,
			gravityMenu,
			filterMenu,
			modeMenu,
			resetMenu,
			pauseMenu,
			sidebar = __webpack_require__(74)(graph),
			setupableMenues,
		// Graph modules
			statistics = webvowl.modules.statistics(),
			focuser = webvowl.modules.focuser(),
			selectionDetailDisplayer = webvowl.modules.selectionDetailsDisplayer(sidebar.updateSelectionInformation),
			datatypeFilter = webvowl.modules.datatypeFilter(),
			subclassFilter = webvowl.modules.subclassFilter(),
			disjointFilter = webvowl.modules.disjointFilter(),
			nodeDegreeFilter = webvowl.modules.nodeDegreeFilter(),
			setOperatorFilter = webvowl.modules.setOperatorFilter(),
			nodeScalingSwitch = webvowl.modules.nodeScalingSwitch(graph),
			compactNotationSwitch = webvowl.modules.compactNotationSwitch(graph),
			pickAndPin = webvowl.modules.pickAndPin();
	
		app.initialize = function () {
			options.graphContainerSelector(graphSelector);
			options.selectionModules().push(focuser);
			options.selectionModules().push(selectionDetailDisplayer);
			options.selectionModules().push(pickAndPin);
			options.filterModules().push(statistics);
			options.filterModules().push(datatypeFilter);
			options.filterModules().push(subclassFilter);
			options.filterModules().push(disjointFilter);
			options.filterModules().push(setOperatorFilter);
			options.filterModules().push(nodeScalingSwitch);
			options.filterModules().push(nodeDegreeFilter);
			options.filterModules().push(compactNotationSwitch);
	
			exportMenu = __webpack_require__(75)(options.graphContainerSelector());
			gravityMenu = __webpack_require__(76)(graph);
			filterMenu = __webpack_require__(77)(graph, datatypeFilter, subclassFilter, disjointFilter, setOperatorFilter, nodeDegreeFilter);
			modeMenu = __webpack_require__(78)(graph, pickAndPin, nodeScalingSwitch, compactNotationSwitch);
			pauseMenu = __webpack_require__(79)(graph);
			resetMenu = __webpack_require__(80)(graph, [gravityMenu, filterMenu, modeMenu,
				focuser, selectionDetailDisplayer, pauseMenu]);
			ontologyMenu = __webpack_require__(81)(loadOntologyFromText);
	
			d3.select(window).on("resize", adjustSize);
	
			// setup all bottom bar modules
			// setupableMenues = [exportMenu, gravityMenu, filterMenu, modeMenu, resetMenu, pauseMenu, sidebar, ontologyMenu];
			setupableMenues = [exportMenu, gravityMenu, filterMenu, modeMenu, resetMenu, pauseMenu, sidebar];
			setupableMenues.forEach(function (menu) {
				menu.setup();
			});
	
			graph.start();
			adjustSize();
		};
	
		function loadOntologyFromText(jsonText, filename, alternativeFilename) {
			pauseMenu.reset();
	
			var data;
			if (jsonText) {
				data = JSON.parse(jsonText);
	
				if (!filename) {
					// First look if an ontology title exists, otherwise take the alternative filename
					var ontologyNames = data.header ? data.header.title : undefined;
					var ontologyName = languageTools.textInLanguage(ontologyNames);
	
					if (ontologyName) {
						filename = ontologyName;
					} else {
						filename = alternativeFilename;
					}
				}
			}
	
			exportMenu.setJsonText(jsonText);
	
			options.data(data);
			graph.reload();
			sidebar.updateOntologyInformation(data, statistics);
	
			exportMenu.setFilename(filename);
		}
	
		function adjustSize() {
			var graphContainer = d3.select(graphSelector),
				svg = graphContainer.select("svg"),
				height = window.innerHeight - 40,
				width = window.innerWidth;// - (window.innerWidth * 0.22);
	
			graphContainer.style("height", height + "px");
			svg.attr("width", width)
				.attr("height", height);
	
			options.width(width)
				.height(height);
			graph.updateStyle();
		}
	
		return app;
	};
	
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 74 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {/**
	 * Contains the logic for the sidebar.
	 * @param graph the graph that belongs to these controls
	 * @returns {{}}
	 */
	module.exports = function (graph) {
	
		var sidebar = {},
			languageTools = webvowl.util.languageTools(),
			elementTools = webvowl.util.elementTools(),
		// Required for reloading when the language changes
			ontologyInfo,
			lastSelectedElement;
	
	
		/**
		 * Setup the menu bar.
		 */
		sidebar.setup = function () {
			setupCollapsing();
		};
	
		function setupCollapsing() {
			// adapted version of this example: http://www.normansblog.de/simple-jquery-accordion/
			function collapseContainers(containers) {
				containers.classed("hidden", true);
			}
	
			function expandContainers(containers) {
				containers.classed("hidden", false);
			}
	
			var triggers = d3.selectAll(".accordion-trigger");
	
			// Collapse all inactive triggers on startup
			collapseContainers(d3.selectAll(".accordion-trigger:not(.accordion-trigger-active) + div"));
	
			triggers.on("click", function () {
				var selectedTrigger = d3.select(this),
					activeTriggers = d3.selectAll(".accordion-trigger-active");
	
				if (selectedTrigger.classed("accordion-trigger-active")) {
					// Collapse the active (which is also the selected) trigger
					collapseContainers(d3.select(selectedTrigger.node().nextElementSibling));
					selectedTrigger.classed("accordion-trigger-active", false);
				} else {
					// Collapse the other trigger ...
					collapseContainers(d3.selectAll(".accordion-trigger-active + div"));
					activeTriggers.classed("accordion-trigger-active", false);
					// ... and expand the selected one
					expandContainers(d3.select(selectedTrigger.node().nextElementSibling));
					selectedTrigger.classed("accordion-trigger-active", true);
				}
			});
		}
	
		/**
		 * Updates the information of the passed ontology.
		 * @param data the graph data
		 * @param statistics the statistics module
		 */
		sidebar.updateOntologyInformation = function (data, statistics) {
			data = data || {};
			ontologyInfo = data.header || {};
	
			updateGraphInformation();
			displayGraphStatistics(data.metrics, statistics);
			displayMetadata(ontologyInfo.other);
	
			// Reset the sidebar selection
			sidebar.updateSelectionInformation(undefined);
	
			setLanguages(ontologyInfo.languages);
		};
	
		function setLanguages(languages) {
			languages = languages || [];
	
			// Put the default and unset label on top of the selection labels
			languages.sort(function (a, b) {
				if (a === webvowl.util.constants().LANG_IRIBASED) {
					return -1;
				} else if (b === webvowl.util.constants().LANG_IRIBASED) {
					return 1;
				}
				if (a === webvowl.util.constants().LANG_UNDEFINED) {
					return -1;
				} else if (b === webvowl.util.constants().LANG_UNDEFINED) {
					return 1;
				}
				return a.localeCompare(b);
			});
	
			var languageSelection = d3.select("#language")
				.on("change", function () {
					graph.language(d3.event.target.value);
					updateGraphInformation();
					sidebar.updateSelectionInformation(lastSelectedElement);
				});
	
			languageSelection.selectAll("option").remove();
			languageSelection.selectAll("option")
				.data(languages)
				.enter().append("option")
				.attr("value", function (d) {
					return d;
				})
				.text(function (d) {
					return d;
				});
	
			if (!trySelectDefaultLanguage(languageSelection, languages, "en")) {
				if (!trySelectDefaultLanguage(languageSelection, languages, webvowl.util.constants().LANG_UNDEFINED)) {
					trySelectDefaultLanguage(languageSelection, languages, webvowl.util.constants().LANG_IRIBASED);
				}
			}
		}
	
		function trySelectDefaultLanguage(selection, languages, language) {
			var langIndex = languages.indexOf(language);
			if (langIndex >= 0) {
				selection.property("selectedIndex", langIndex);
				graph.language(language);
				return true;
			}
	
			return false;
		}
	
		function updateGraphInformation() {
			var title = languageTools.textInLanguage(ontologyInfo.title, graph.language());
			d3.select("#title").text(title || "No title available");
			d3.select("#about").attr("href", ontologyInfo.iri).attr("target", "_blank").text(ontologyInfo.iri);
			d3.select("#version").text(ontologyInfo.version || "--");
			var authors = ontologyInfo.author;
			if (typeof authors === "string") {
				// Stay compatible with author info as strings after change in january 2015
				d3.select("#authors").text(authors);
			} else if (authors instanceof Array) {
				d3.select("#authors").text(authors.join(", "));
			} else {
				d3.select("#authors").text("--");
			}
	
			var description = languageTools.textInLanguage(ontologyInfo.description, graph.language());
			d3.select("#description").text(description || "No description available.");
		}
	
		function displayGraphStatistics(deliveredMetrics, statistics) {
			// Metrics are optional and may be undefined
			deliveredMetrics = deliveredMetrics || {};
	
			d3.select("#classCount")
				.text(deliveredMetrics.classCount || statistics.classCount());
			d3.select("#objectPropertyCount")
				.text(deliveredMetrics.objectPropertyCount || statistics.objectPropertyCount());
			d3.select("#datatypePropertyCount")
				.text(deliveredMetrics.datatypePropertyCount || statistics.datatypePropertyCount());
			d3.select("#individualCount")
				.text(deliveredMetrics.totalIndividualCount || statistics.totalIndividualCount());
			d3.select("#nodeCount")
				.text(statistics.nodeCount());
			d3.select("#edgeCount")
				.text(statistics.edgeCount());
		}
	
		function displayMetadata(metadata) {
			var container = d3.select("#ontology-metadata");
			container.selectAll("*").remove();
	
			listAnnotations(container, metadata);
	
			if (container.selectAll(".annotation").size() <= 0) {
				container.append("p").text("No annotations available.");
			}
		}
	
		function listAnnotations(container, annotationObject) {
			annotationObject = annotationObject || {};  //todo
	
			// Collect the annotations in an array for simpler processing
			var annotations = [];
			for (var annotation in annotationObject) {
				if (annotationObject.hasOwnProperty(annotation)) {
					annotations.push(annotationObject[annotation][0]);
				}
			}
	
			container.selectAll(".annotation").remove();
			container.selectAll(".annotation").data(annotations).enter().append("p")
				.classed("annotation", true)
				.classed("statisticDetails", true)
				.text(function (d) {
					return d.identifier + ":";
				})
				.append("span")
				.each(function (d) {
					appendIriLabel(d3.select(this), d.value, d.type === "iri" ? d.value : undefined);
				});
		}
	
		/**
		 * Update the information of the selected node.
		 * @param selectedElement the selection or null if nothing is selected
		 */
		sidebar.updateSelectionInformation = function (selectedElement) {
			lastSelectedElement = selectedElement;
	
			// Click event was prevented when dragging
			if (d3.event && d3.event.defaultPrevented) {
				return;
			}
	
	
			var isTriggerActive = d3.select("#selection-details-trigger").classed("accordion-trigger-active");
			if (selectedElement && !isTriggerActive) {
				d3.select("#selection-details-trigger").node().click();
			} else if (!selectedElement && isTriggerActive) {
				showSelectionAdvice();
				return;
			}
	
			if (elementTools.isProperty(selectedElement)) {
				displayPropertyInformation(selectedElement);
			} else if (elementTools.isNode(selectedElement)) {
				displayNodeInformation(selectedElement);
			}
		};
	
		function showSelectionAdvice() {
			setSelectionInformationVisibility(false, false, true);
		}
	
		function setSelectionInformationVisibility(showClasses, showProperties, showAdvice) {
			d3.select("#classSelectionInformation").classed("hidden", !showClasses);
			d3.select("#propertySelectionInformation").classed("hidden", !showProperties);
			d3.select("#noSelectionInformation").classed("hidden", !showAdvice);
		}
	
		function displayPropertyInformation(property) {
			showPropertyInformations();
	
			setIriLabel(d3.select("#propname"), property.labelForCurrentLanguage(), property.iri());
			d3.select("#typeProp").text(property.type());
	
			if (property.inverse() !== undefined) {
				d3.select("#inverse").classed("hidden", false);
				setIriLabel(d3.select("#inverse span"), property.inverse().labelForCurrentLanguage(), property.inverse().iri());
			} else {
				d3.select("#inverse").classed("hidden", true);
			}
	
			var equivalentIriSpan = d3.select("#propEquivUri");
			listNodeArray(equivalentIriSpan, property.equivalents());
	
			listNodeArray(d3.select("#subproperties"), property.subproperties());
			listNodeArray(d3.select("#superproperties"), property.superproperties());
	
			if (property.minCardinality() !== undefined) {
				d3.select("#infoCardinality").classed("hidden", true);
				d3.select("#minCardinality").classed("hidden", false);
				d3.select("#minCardinality span").text(property.minCardinality());
				d3.select("#maxCardinality").classed("hidden", false);
	
				if (property.maxCardinality() !== undefined) {
					d3.select("#maxCardinality span").text(property.maxCardinality());
				} else {
					d3.select("#maxCardinality span").text("*");
				}
	
			} else if (property.cardinality() !== undefined) {
				d3.select("#minCardinality").classed("hidden", true);
				d3.select("#maxCardinality").classed("hidden", true);
				d3.select("#infoCardinality").classed("hidden", false);
				d3.select("#infoCardinality span").text(property.cardinality());
			} else {
				d3.select("#infoCardinality").classed("hidden", true);
				d3.select("#minCardinality").classed("hidden", true);
				d3.select("#maxCardinality").classed("hidden", true);
			}
	
			setIriLabel(d3.select("#domain"), property.domain().labelForCurrentLanguage(), property.domain().iri());
			setIriLabel(d3.select("#range"), property.range().labelForCurrentLanguage(), property.range().iri());
	
			displayAttributes(property.attributes(), d3.select("#propAttributes"));
	
			setTextAndVisibility(d3.select("#propDescription"), property.descriptionForCurrentLanguage());
			setTextAndVisibility(d3.select("#propComment"), property.commentForCurrentLanguage());
	
			listAnnotations(d3.select("#propertySelectionInformation"), property.annotations());
		}
	
		function showPropertyInformations() {
			setSelectionInformationVisibility(false, true, false);
		}
	
		function setIriLabel(element, name, iri) {
			element.selectAll("*").remove();
			appendIriLabel(element, name, iri);
		}
	
		function appendIriLabel(element, name, iri) {
			var tag;
	
			if (iri) {
				tag = element.append("a")
					.attr("href", iri)
					.attr("title", iri)
					.attr("target", "_blank");
			} else {
				tag = element.append("span");
			}
			tag.text(name);
		}
	
		function displayAttributes(attributes, textSpan) {
			var spanParent = d3.select(textSpan.node().parentNode);
	
			if (attributes && attributes.length > 0) {
				// Remove redundant redundant attributes for sidebar
				removeElementFromArray("object", attributes);
				removeElementFromArray("datatype", attributes);
				removeElementFromArray("rdf", attributes);
			}
	
			if (attributes && attributes.length > 0) {
				textSpan.text(attributes.join(", "));
	
				spanParent.classed("hidden", false);
			} else {
				spanParent.classed("hidden", true);
			}
		}
	
		function removeElementFromArray(element, array) {
			var index = array.indexOf(element);
			if (index > -1) {
				array.splice(index, 1);
			}
		}
	
		function displayNodeInformation(node) {
			showClassInformations();
	
			setIriLabel(d3.select("#name"), node.labelForCurrentLanguage(), node.iri());
	
			/* Equivalent stuff. */
			var equivalentIriSpan = d3.select("#classEquivUri");
			listNodeArray(equivalentIriSpan, node.equivalents());
	
			d3.select("#typeNode").text(node.type());
			listNodeArray(d3.select("#individuals"), node.individuals());
	
			/* Disjoint stuff. */
			var disjointNodes = d3.select("#disjointNodes");
			var disjointNodesParent = d3.select(disjointNodes.node().parentNode);
	
			if (node.disjointWith() !== undefined) {
				disjointNodes.selectAll("*").remove();
	
				node.disjointWith().forEach(function (element, index) {
					if (index > 0) {
						disjointNodes.append("span").text(", ");
					}
					appendIriLabel(disjointNodes, element.labelForCurrentLanguage(), element.iri());
				});
	
				disjointNodesParent.classed("hidden", false);
			} else {
				disjointNodesParent.classed("hidden", true);
			}
	
			displayAttributes(node.attributes(), d3.select("#classAttributes"));
	
			setTextAndVisibility(d3.select("#nodeDescription"), node.descriptionForCurrentLanguage());
			setTextAndVisibility(d3.select("#nodeComment"), node.commentForCurrentLanguage());
	
			listAnnotations(d3.select("#classSelectionInformation"), node.annotations());
		}
	
		function showClassInformations() {
			setSelectionInformationVisibility(true, false, false);
		}
	
		function listNodeArray(textSpan, nodes) {
			var spanParent = d3.select(textSpan.node().parentNode);
	
			if (nodes && nodes.length) {
				textSpan.selectAll("*").remove();
				nodes.forEach(function (element, index) {
					if (index > 0) {
						textSpan.append("span").text(", ");
					}
					appendIriLabel(textSpan, element.labelForCurrentLanguage(), element.iri());
				});
	
				spanParent.classed("hidden", false);
			} else {
				spanParent.classed("hidden", true);
			}
		}
	
		function setTextAndVisibility(label, value) {
			var parentNode = d3.select(label.node().parentNode);
			var hasValue = !!value;
			if (value) {
				label.text(value);
			}
			parentNode.classed("hidden", !hasValue);
		}
	
	
		return sidebar;
	};
	
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 75 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {/**
	 * Contains the logic for the export button.
	 *
	 * @param graphSelector the associated graph svg selector
	 * @returns {{}}
	 */
	module.exports = function (graphSelector) {
	
		var exportMenu = {},
			exportSvgButton,
			exportFilename,
			exportJsonButton,
			exportableJsonText;
	
	
		/**
		 * Adds the export button to the website.
		 */
		exportMenu.setup = function () {
			exportSvgButton = d3.select("#exportSvg")
				.on("click", exportSvg);
			exportJsonButton = d3.select("#exportJson")
				.on("click", exportJson);
		};
	
		exportMenu.setFilename = function (filename) {
			exportFilename = filename || "export";
		};
	
		exportMenu.setJsonText = function (jsonText) {
			exportableJsonText = jsonText;
		};
	
		function exportSvg() {
			// Get the d3js SVG element
			var graphSvg = d3.select(graphSelector).select("svg"),
				graphSvgCode,
				escapedGraphSvgCode,
				dataURI;
	
			// inline the styles, so that the exported svg code contains the css rules
			inlineVowlStyles();
			hideNonExportableElements();
	
			graphSvgCode = graphSvg.attr("version", 1.1)
				.attr("xmlns", "http://www.w3.org/2000/svg")
				.node().parentNode.innerHTML;
	
			// Insert the reference to VOWL
			graphSvgCode = "<!-- Created with WebVOWL (version " + webvowl.version + ")" +
			", http://vowl.visualdataweb.org -->\n" + graphSvgCode;
	
			escapedGraphSvgCode = escapeUnicodeCharacters(graphSvgCode);
			//btoa(); Creates a base-64 encoded ASCII string from a "string" of binary data.
			dataURI = "data:image/svg+xml;base64," + btoa(escapedGraphSvgCode);
	
			exportSvgButton.attr("href", dataURI)
				.attr("download", exportFilename + ".svg");
	
			// remove graphic styles for interaction to go back to normal
			removeVowlInlineStyles();
			showNonExportableElements();
		}
	
		function escapeUnicodeCharacters(text) {
			var textSnippets = [],
				i, textLength = text.length,
				character,
				charCode;
	
			for (i = 0; i < textLength; i++) {
				character = text.charAt(i);
				charCode = character.charCodeAt(0);
	
				if (charCode < 128) {
					textSnippets.push(character);
				} else {
					textSnippets.push("&#" + charCode + ";");
				}
			}
	
			return textSnippets.join("");
		}
	
		function inlineVowlStyles() {
			d3.selectAll(".text").style("font-family", "Helvetica, Arial, sans-serif").style("font-size", "12px");
			d3.selectAll(".subtext").style("font-size", "9px");
			d3.selectAll(".text.instance-count").style("fill", "#666");
			d3.selectAll(".external + text .instance-count").style("fill", "#aaa");
			d3.selectAll(".cardinality").style("font-size", "10px");
			d3.selectAll(".text, .embedded").style("pointer-events", "none");
			d3.selectAll(".class, .object, .disjoint, .objectproperty, .disjointwith, .equivalentproperty, .transitiveproperty, .functionalproperty, .inversefunctionalproperty, .symmetricproperty").style("fill", "#acf");
			d3.selectAll(".label .datatype, .datatypeproperty").style("fill", "#9c6");
			d3.selectAll(".rdf, .rdfproperty").style("fill", "#c9c");
			d3.selectAll(".literal, .node .datatype").style("fill", "#fc3");
			d3.selectAll(".deprecated, .deprecatedproperty").style("fill", "#ccc");
			d3.selectAll(".external, .externalproperty").style("fill", "#36c");
			d3.selectAll("path, .nofill").style("fill", "none");
			d3.selectAll(".symbol").style("fill", "#69c");
			d3.selectAll(".arrowhead, marker path").style("fill", "#000");
			d3.selectAll(".class, path, line, .fineline").style("stroke", "#000");
			d3.selectAll(".white, .subclass, .dottedMarker path, .subclassproperty, .external + text").style("fill", "#fff");
			d3.selectAll(".class.hovered, .property.hovered, path.arrowhead.hovered, .cardinality.hovered, .normalMarker path.hovered, .cardinality.focused, .normalMarker path.focused, circle.pin").style("fill", "#f00").style("cursor", "pointer");
			d3.selectAll(".focused, path.hovered").style("stroke", "#f00");
			d3.selectAll(".label .indirectHighlighting, .feature:hover").style("fill", "#f90");
			d3.selectAll(".class, path, line").style("stroke-width", "2");
			d3.selectAll(".fineline").style("stroke-width", "1");
			d3.selectAll(".special").style("stroke-dasharray", "8");
			d3.selectAll(".dotted").style("stroke-dasharray", "3");
			d3.selectAll("rect.focused, circle.focused").style("stroke-width", "4px");
			d3.selectAll(".nostroke").style("stroke", "none");
			d3.selectAll("#width-test").style("position", "absolute").style("float", "left").style("white-space", "nowrap").style("visibility", "hidden");
			d3.selectAll("marker path").style("stroke-dasharray", "50");
		}
	
		/**
		 * For example the pin of the pick&pin module should be invisible in the exported graphic.
		 */
		function hideNonExportableElements() {
			d3.selectAll(".hidden-in-export").style("display", "none");
		}
	
		function removeVowlInlineStyles() {
			d3.selectAll(".text, .subtext, .text.instance-count, .external + text .instance-count, .cardinality, .text, .embedded, .class, .object, .disjoint, .objectproperty, .disjointwith, .equivalentproperty, .transitiveproperty, .functionalproperty, .inversefunctionalproperty, .symmetricproperty, .label .datatype, .datatypeproperty, .rdf, .rdfproperty, .literal, .node .datatype, .deprecated, .deprecatedproperty, .external, .externalproperty, path, .nofill, .symbol, .arrowhead, marker path, .class, path, line, .fineline, .white, .subclass, .dottedMarker path, .subclassproperty, .external + text, .class.hovered, .property.hovered, path.arrowhead.hovered, .cardinality.hovered, .normalMarker path.hovered, .cardinality.focused, .normalMarker path.focused, circle.pin, .focused, path.hovered, .label .indirectHighlighting, .feature:hover, .class, path, line, .fineline, .special, .dotted, rect.focused, circle.focused, .nostroke, #width-test, marker path").attr("style", null);
		}
	
		function showNonExportableElements() {
			d3.selectAll(".hidden-in-export").style("display", null);
		}
	
		function exportJson() {
			if (!exportableJsonText) {
				alert("No graph data available.");
				// Stop the redirection to the path of the href attribute
				d3.event.preventDefault();
				return;
			}
	
			var dataURI = "data:text/json;charset=utf-8," + encodeURIComponent(exportableJsonText);
			exportJsonButton.attr("href", dataURI)
				.attr("download", exportFilename + ".json");
		}
	
		return exportMenu;
	};
	
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 76 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {/**
	 * Contains the logic for setting up the gravity sliders.
	 *
	 * @param graph the associated webvowl graph
	 * @returns {{}}
	 */
	module.exports = function (graph) {
	
		var gravityMenu = {},
			sliders = [],
			options = graph.graphOptions(),
			defaultCharge = options.charge();
	
	
		/**
		 * Adds the gravity sliders to the website.
		 */
		gravityMenu.setup = function () {
			addDistanceSlider("#classSliderOption", "class", "Class distance", options.classDistance);
			addDistanceSlider("#datatypeSliderOption", "datatype", "Datatype distance", options.datatypeDistance);
		};
	
		function addDistanceSlider(selector, identifier, label, distanceFunction) {
			var defaultLinkDistance = distanceFunction();
	
			var sliderContainer,
				sliderValueLabel;
	
			sliderContainer = d3.select(selector)
				.append("div")
				.datum({distanceFunction: distanceFunction}) // connect the options-function with the slider
				.classed("distanceSliderContainer", true);
	
			var slider = sliderContainer.append("input")
				.attr("id", identifier + "DistanceSlider")
				.attr("type", "range")
				.attr("min", 10)
				.attr("max", 600)
				.attr("value", distanceFunction())
				.attr("step", 10);
	
			sliderContainer.append("label")
				.classed("description", true)
				.attr("for", identifier + "DistanceSlider")
				.text(label);
	
			sliderValueLabel = sliderContainer.append("label")
				.classed("value", true)
				.attr("for", identifier + "DistanceSlider")
				.text(distanceFunction());
	
			// Store slider for easier resetting
			sliders.push(slider);
	
			slider.on("input", function () {
				var distance = slider.property("value");
				distanceFunction(distance);
				adjustCharge(defaultLinkDistance);
				sliderValueLabel.text(distance);
				graph.updateStyle();
			});
		}
	
		function adjustCharge(defaultLinkDistance) {
			var greaterDistance = Math.max(options.classDistance(), options.datatypeDistance()),
				ratio = greaterDistance / defaultLinkDistance,
				newCharge = defaultCharge * ratio;
	
			options.charge(newCharge);
		}
	
		/**
		 * Resets the gravity sliders to their default.
		 */
		gravityMenu.reset = function () {
			sliders.forEach(function (slider) {
				slider.property("value", function (d) {
					// Simply reload the distance from the options
					return d.distanceFunction();
				});
				slider.on("input")();
			});
		};
	
	
		return gravityMenu;
	};
	
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 77 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {/**
	 * Contains the logic for connecting the filters with the website.
	 *
	 * @param graph required for calling a refresh after a filter change
	 * @param datatypeFilter filter for all datatypes
	 * @param subclassFilter filter for all subclasses
	 * @param disjointFilter filter for all disjoint with properties
	 * @param setOperatorFilter filter for all set operators with properties
	 * @param nodeDegreeFilter filters nodes by their degree
	 * @returns {{}}
	 */
	module.exports = function (graph, datatypeFilter, subclassFilter, disjointFilter, setOperatorFilter, nodeDegreeFilter) {
	
		var filterMenu = {},
			checkboxData = [],
			degreeSlider;
	
	
		/**
		 * Connects the website with graph filters.
		 */
		filterMenu.setup = function () {
			addFilterItem(datatypeFilter, "datatype", "Datatype prop.", "#datatypeFilteringOption");
			addFilterItem(subclassFilter, "subclass", "Solitary subclass.", "#subclassFilteringOption");
			addFilterItem(disjointFilter, "disjoint", "Disjointness info", "#disjointFilteringOption");
			addFilterItem(setOperatorFilter, "setoperator", "Set operators", "#setOperatorFilteringOption");
	
			addNodeDegreeFilter("#nodeDegreeFilteringOption");
		};
	
	
		function addFilterItem(filter, identifier, pluralNameOfFilteredItems, selector) {
			var filterContainer,
				filterCheckbox;
	
			filterContainer = d3.select(selector)
				.append("div")
				.classed("checkboxContainer", true);
	
			filterCheckbox = filterContainer.append("input")
				.classed("filterCheckbox", true)
				.attr("id", identifier + "FilterCheckbox")
				.attr("type", "checkbox")
				.property("checked", filter.enabled());
	
			// Store for easier resetting
			checkboxData.push({checkbox: filterCheckbox, defaultState: filter.enabled()});
	
			filterCheckbox.on("click", function () {
				// There might be no parameters passed because of a manual
				// invocation when resetting the filters
				var isEnabled = filterCheckbox.property("checked");
				filter.enabled(isEnabled);
				graph.update();
			});
	
			filterContainer.append("label")
				.attr("for", identifier + "FilterCheckbox")
				.text(pluralNameOfFilteredItems);
		}
	
		function addNodeDegreeFilter(selector) {
			nodeDegreeFilter.setMaxDegreeSetter(function (maxDegree) {
				degreeSlider.attr("max", maxDegree);
				degreeSlider.property("value", Math.min(maxDegree, degreeSlider.property("value")));
				//  degreeSlider.property("value", 1);  //Hide nodes with no connections
			});
	
			nodeDegreeFilter.setDegreeQueryFunction(function () {
				return degreeSlider.property("value");
			});
	
			var sliderContainer,
				sliderValueLabel;
	
			sliderContainer = d3.select(selector)
				.append("div")
				.classed("distanceSliderContainer", true);
	
			degreeSlider = sliderContainer.append("input")
				.attr("id", "nodeDegreeDistanceSlider")
				.attr("type", "range")
				.attr("min", 0)
				.attr("step", 1);
	
			sliderContainer.append("label")
				.classed("description", true)
				.attr("for", "nodeDegreeDistanceSlider")
				.text("Degree of collapsing");
	
			sliderValueLabel = sliderContainer.append("label")
				.classed("value", true)
				.attr("for", "nodeDegreeDistanceSlider")
				.text(0);
	
			degreeSlider.on("change", function () {
				graph.update();
			});
	
			degreeSlider.on("input", function () {
				var degree = degreeSlider.property("value");
				sliderValueLabel.text(degree);
			});
		}
	
		/**
		 * Resets the filters (and also filtered elements) to their default.
		 */
		filterMenu.reset = function () {
			checkboxData.forEach(function (checkboxData) {
				var checkbox = checkboxData.checkbox,
					enabledByDefault = checkboxData.defaultState,
					isChecked = checkbox.property("checked");
	
				if (isChecked !== enabledByDefault) {
					checkbox.property("checked", enabledByDefault);
					// Call onclick event handlers programmatically
					checkbox.on("click")();
				}
			});
	
			degreeSlider.property("value", 0);
			degreeSlider.on("change")();
			degreeSlider.on("input")();
		};
	
	
		return filterMenu;
	};
	
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 78 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {/**
	 * Contains the logic for connecting the modes with the website.
	 *
	 * @param graph the graph that belongs to these controls
	 * @param pickAndPin mode for picking and pinning of nodes
	 * @param nodeScaling mode for toggling node scaling
	 * @param compactNotation mode for toggling the compact node
	 * @returns {{}}
	 */
	module.exports = function (graph, pickAndPin, nodeScaling, compactNotation) {
	
		var modeMenu = {},
			checkboxes = [];
	
	
		/**
		 * Connects the website with the available graph modes.
		 */
		modeMenu.setup = function () {
			addModeItem(pickAndPin, "pickandpin", "Pick & Pin", "#pickAndPinOption", false);
			addModeItem(nodeScaling, "nodescaling", "Node Scaling", "#nodeScalingOption", true);
			addModeItem(compactNotation, "compactnotation", "Compact Notation", "#compactNotationOption", true);
		};
	
		function addModeItem(module, identifier, modeName, selector, updateGraphOnClick) {
			var moduleOptionContainer,
				moduleCheckbox;
	
			moduleOptionContainer = d3.select(selector)
				.append("div")
				.classed("checkboxContainer", true)
				.datum({module: module, defaultState: module.enabled()});
	
			moduleCheckbox = moduleOptionContainer.append("input")
				.classed("moduleCheckbox", true)
				.attr("id", identifier + "ModuleCheckbox")
				.attr("type", "checkbox")
				.property("checked", module.enabled());
	
			// Store for easier resetting all modes
			checkboxes.push(moduleCheckbox);
	
			moduleCheckbox.on("click", function (d) {
				var isEnabled = moduleCheckbox.property("checked");
				d.module.enabled(isEnabled);
	
				if (updateGraphOnClick) {
					graph.update();
				}
			});
	
			moduleOptionContainer.append("label")
				.attr("for", identifier + "ModuleCheckbox")
				.text(modeName);
		}
	
		/**
		 * Resets the modes to their default.
		 */
		modeMenu.reset = function () {
			checkboxes.forEach(function (checkbox) {
				var defaultState = checkbox.datum().defaultState,
					isChecked = checkbox.property("checked");
	
				if (isChecked !== defaultState) {
					checkbox.property("checked", defaultState);
					// Call onclick event handlers programmatically
					checkbox.on("click")(checkbox.datum());
				}
	
				// Reset the module that is connected with the checkbox
				checkbox.datum().module.reset();
			});
		};
	
	
		return modeMenu;
	};
	
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 79 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {/**
	 * Contains the logic for the pause and resume button.
	 *
	 * @param graph the associated webvowl graph
	 * @returns {{}}
	 */
	module.exports = function (graph) {
	
		var pauseMenu = {},
			pauseButton;
	
	
		/**
		 * Adds the pause button to the website.
		 */
		pauseMenu.setup = function () {
			pauseButton = d3.select("#pause-button")
				.datum({paused: false})
				.on("click", function (d) {
					if (d.paused) {
						graph.unfreeze();
					} else {
						graph.freeze();
					}
					d.paused = !d.paused;
					updatePauseButton();
				});
	
			// Set these properties the first time manually
			updatePauseButton();
		};
	
		function updatePauseButton() {
			updatePauseButtonClass();
			updatePauseButtonText();
		}
	
		function updatePauseButtonClass() {
			pauseButton.classed("paused", function (d) {
				return d.paused;
			});
		}
	
		function updatePauseButtonText() {
			if (pauseButton.datum().paused) {
				pauseButton.text("Resume");
			} else {
				pauseButton.text("Pause");
			}
		}
	
		pauseMenu.reset = function () {
			// Simulate resuming
			pauseButton.datum().paused = false;
			graph.unfreeze();
			updatePauseButton();
		};
	
	
		return pauseMenu;
	};
	
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 80 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {/**
	 * Contains the logic for the reset button.
	 *
	 * @param graph the associated webvowl graph
	 * @param resettableModules modules that can be resetted
	 * @returns {{}}
	 */
	module.exports = function (graph, resettableModules) {
	
		var resetMenu = {},
			options = graph.graphOptions(),
			untouchedOptions = webvowl.options();
	
	
		/**
		 * Adds the reset button to the website.
		 */
		resetMenu.setup = function () {
			d3.select("#reset-button").on("click", resetGraph);
		};
	
		function resetGraph() {
			options.classDistance(untouchedOptions.classDistance());
			options.datatypeDistance(untouchedOptions.datatypeDistance());
			options.charge(untouchedOptions.charge());
			options.gravity(untouchedOptions.gravity());
			options.linkStrength(untouchedOptions.linkStrength());
			graph.reset();
	
			resettableModules.forEach(function (module) {
				module.reset();
			});
	
			graph.updateStyle();
		}
	
	
		return resetMenu;
	};
	
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 81 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {/**
	 * Contains the logic for the ontology listing and conversion.
	 *
	 * @returns {{}}
	 */
	module.exports = function (loadOntologyFromText) {
	
		var ontologyMenu = {},
			defaultJsonName = "foaf", // This file is loaded by default
		// Selections for the app
			loadingError = d3.select("#loading-error"),
			loadingProgress = d3.select("#loading-progress"),
			ontologyMenuTimeout,
			cachedConversions = {};
	
		ontologyMenu.setup = function () {
			setupConverterButtons();
			setupUploadButton();
	
			var descriptionButton = d3.select("#error-description-button").datum({open: false});
			descriptionButton.on("click", function (data) {
				var errorContainer = d3.select("#error-description-container");
				var errorDetailsButton = d3.select(this);
	
				// toggle the state
				data.open = !data.open;
				var descriptionVisible = data.open;
				if (descriptionVisible) {
					errorDetailsButton.text("Hide error details");
				} else {
					errorDetailsButton.text("Show error details");
				}
				errorContainer.classed("hidden", !descriptionVisible);
			});
	
			setupUriListener();
		};
	
	
		function setupUriListener() {
			// parse the url initially
			parseUrlAndLoadOntology();
	
			// reload ontology when hash parameter gets changed manually
			d3.select(window).on("hashchange", function () {
				var oldURL = d3.event.oldURL, newURL = d3.event.newURL;
	
				if (oldURL !== newURL) {
					// don't reload when just the hash parameter gets appended
					if (newURL === oldURL + "#") {
						return;
					}
	
					updateNavigationHrefs();
					parseUrlAndLoadOntology();
				}
			});
	
			updateNavigationHrefs();
		}
	
		/**
		 * Quick fix: update all anchor tags that are used as buttons because a click on them
		 * changes the url and this will load an other ontology.
		 */
		function updateNavigationHrefs() {
			d3.selectAll("#optionsMenu > li > a").attr("href", location.hash || "#");
		}
	
		function parseUrlAndLoadOntology() {
			// slice the "#" character
			var hashParameter = location.hash.slice(1);
	
			if (!hashParameter) {
				hashParameter = defaultJsonName;
			}
	
			var ontologyOptions = d3.selectAll(".select li").classed("selected-ontology", false);
	
			// IRI parameter
			var iriKey = "iri=";
			var fileKey = "file=";
			if (hashParameter.substr(0, fileKey.length) === fileKey) {
				var filename = decodeURIComponent(hashParameter.slice(fileKey.length));
				loadOntologyFromFile(filename);
			} else if (hashParameter.substr(0, iriKey.length) === iriKey) {
				var iri = decodeURIComponent(hashParameter.slice(iriKey.length));
				loadOntologyFromUri("converter.php?iri=" + encodeURIComponent(iri), iri);
	
				d3.select("#converter-option").classed("selected-ontology", true);
			} else {
				// id of an existing ontology as parameter
				loadOntologyFromUri(__webpack_require__(82)("./" + hashParameter + ".json"), hashParameter);
	
				ontologyOptions.each(function () {
					var ontologyOption = d3.select(this);
					if (ontologyOption.select("a").size() > 0) {
	
						if (ontologyOption.select("a").attr("href") === "#" + hashParameter) {
							ontologyOption.classed("selected-ontology", true);
						}
					}
				});
			}
		}
	
		function loadOntologyFromUri(relativePath, requestedUri) {
			var cachedOntology = cachedConversions[relativePath];
			var trimmedRequestedUri = requestedUri.replace(/\/$/g, "");
			var filename = trimmedRequestedUri.slice(trimmedRequestedUri.lastIndexOf("/") + 1);
	
	
			if (cachedOntology) {
				loadOntologyFromText(cachedOntology, undefined, filename);
				setLoadingStatus(true);
			} else {
				displayLoadingIndicators();
				d3.xhr(relativePath, "application/json", function (error, request) {
					var loadingSuccessful = !error;
					var errorInfo;
	
					var jsonText;
					if (loadingSuccessful) {
						jsonText = request.responseText;
						cachedConversions[relativePath] = jsonText;
					} else {
						if (error.status === 404) {
							errorInfo = "Connection to the OWL2VOWL interface could not be established.";
						}
					}
	
					loadOntologyFromText(jsonText, undefined, filename);
	
					setLoadingStatus(loadingSuccessful, error ? error.response : undefined, errorInfo);
					hideLoadingInformations();
				});
			}
		}
	
		function setupConverterButtons() {
			var iriConverterButton = d3.select("#iri-converter-button");
			var iriConverterInput = d3.select("#iri-converter-input");
	
			iriConverterInput.on("input", function () {
				keepOntologySelectionOpenShortly();
	
				var inputIsEmpty = iriConverterInput.property("value") === "";
				iriConverterButton.attr("disabled", inputIsEmpty || undefined);
			}).on("click", function () {
				keepOntologySelectionOpenShortly();
			});
	
			d3.select("#iri-converter-form").on("submit", function () {
				location.hash = "iri=" + iriConverterInput.property("value");
				iriConverterInput.property("value", "");
				iriConverterInput.on("input")();
	
				// abort the form submission because we set the hash parameter manually to prevent the ? attached in chrome
				d3.event.preventDefault();
				return false;
			});
		}
	
		function setupUploadButton() {
			var input = d3.select("#file-converter-input"),
				inputLabel = d3.select("#file-converter-label"),
				uploadButton = d3.select("#file-converter-button");
	
			input.on("change", function () {
				var selectedFiles = input.property("files");
				if (selectedFiles.length <= 0) {
					inputLabel.text("Please select a file");
					uploadButton.property("disabled", true);
				} else {
					inputLabel.text(selectedFiles[0].name);
					uploadButton.property("disabled", false);
	
					keepOntologySelectionOpenShortly();
				}
			});
	
			uploadButton.on("click", function () {
				var selectedFile = input.property("files")[0];
				if (!selectedFile) {
					return false;
				}
				var newHashParameter = "file=" + selectedFile.name;
				// Trigger the reupload manually, because the iri is not changing
				if (location.hash === "#" + newHashParameter) {
					loadOntologyFromFile();
				} else {
					location.hash = newHashParameter;
				}
			});
		}
	
		function loadOntologyFromFile(filename) {
			var cachedOntology = cachedConversions[filename];
			if (cachedOntology) {
				loadOntologyFromText(cachedOntology, filename);
				setLoadingStatus(true);
				return;
			}
	
			var selectedFile = d3.select("#file-converter-input").property("files")[0];
			// No selection -> this was triggered by the iri. Unequal names -> reuploading another file
			if (!selectedFile || (filename && (filename !== selectedFile.name))) {
				loadOntologyFromText(undefined, undefined);
				setLoadingStatus(false, undefined, "No cached version of \"" + filename + "\" was found. Please reupload the file.");
				return;
			} else {
				filename = selectedFile.name;
			}
	
			if (filename.match(/\.json$/)) {
				loadFromJson(selectedFile, filename);
			} else {
				loadFromOntology(selectedFile, filename);
			}
		}
	
		function loadFromJson(file, filename) {
			var reader = new FileReader();
			reader.readAsText(file);
			reader.onload = function () {
				loadOntologyFromTextAndTrimFilename(reader.result, filename);
				setLoadingStatus(true);
			};
		}
	
		function loadFromOntology(selectedFile, filename) {
			var uploadButton = d3.select("#file-converter-button");
	
			displayLoadingIndicators();
			uploadButton.property("disabled", true);
	
			var formData = new FormData();
			formData.append("ontology", selectedFile);
	
			var xhr = new XMLHttpRequest();
			xhr.open("POST", "converter.php", true);
	
			xhr.onload = function () {
				uploadButton.property("disabled", false);
	
				if (xhr.status === 200) {
					loadOntologyFromTextAndTrimFilename(xhr.responseText, filename);
					cachedConversions[filename] = xhr.responseText;
				} else {
					loadOntologyFromText(undefined, undefined);
					setLoadingStatus(false, xhr.responseText);
				}
				hideLoadingInformations();
			};
	
			xhr.send(formData);
		}
	
		function loadOntologyFromTextAndTrimFilename(text, filename) {
			var trimmedFilename = filename.split(".")[0];
			loadOntologyFromText(text, trimmedFilename);
		}
	
		function keepOntologySelectionOpenShortly() {
			// Events in the menu should not be considered
			var ontologySelection = d3.select("#select .toolTipMenu");
			ontologySelection.on("click", function () {
				d3.event.stopPropagation();
			}).on("keydown", function () {
				d3.event.stopPropagation();
			});
	
			ontologySelection.style("display", "block");
	
			function disableKeepingOpen() {
				ontologySelection.style("display", undefined);
	
				clearTimeout(ontologyMenuTimeout);
				d3.select(window).on("click", undefined).on("keydown", undefined);
				ontologySelection.on("mouseover", undefined);
			}
	
			// Clear the timeout to handle fast calls of this function
			clearTimeout(ontologyMenuTimeout);
			ontologyMenuTimeout = setTimeout(function () {
				disableKeepingOpen();
			}, 3000);
	
			// Disable forced open selection on interaction
			d3.select(window).on("click", function () {
				disableKeepingOpen();
			}).on("keydown", function () {
				disableKeepingOpen();
			});
	
			ontologySelection.on("mouseover", function () {
				disableKeepingOpen();
			});
		}
	
	
		function displayLoadingIndicators() {
			loadingError.classed("hidden", true);
			loadingProgress.classed("hidden", false);
		}
	
		function setLoadingStatus(success, description, information) {
			loadingError.classed("hidden", success);
	
			var errorInfo = d3.select("#error-info");
			if (information) {
				errorInfo.text(information);
			} else {
				errorInfo.html("Ontology could not be loaded.<br>Is it a valid OWL ontology? Please check with <a target=\"_blank\"" +
				"href=\"http://mowl-power.cs.man.ac.uk:8080/validator/\">OWL Validator</a>.");
			}
	
			var descriptionMissing = !description;
			var descriptionVisible = d3.select("#error-description-button").classed("hidden", descriptionMissing).datum().open;
			d3.select("#error-description-container").classed("hidden", descriptionMissing || !descriptionVisible);
			d3.select("#error-description").text(description || "");
		}
	
		function hideLoadingInformations() {
			loadingProgress.classed("hidden", true);
		}
	
		return ontologyMenu;
	};
	
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 82 */
/***/ function(module, exports, __webpack_require__) {

	var map = {
		"./TranscomOntology.json": 83,
		"./TranscomOntologySmall.json": 84,
		"./benchmark.json": 85,
		"./foaf.json": 86,
		"./goodrelations.json": 87,
		"./muto.json": 88,
		"./ontovibe.json": 89,
		"./personasonto.json": 90,
		"./sioc.json": 91,
		"./template.json": 92
	};
	function webpackContext(req) {
		return __webpack_require__(webpackContextResolve(req));
	};
	function webpackContextResolve(req) {
		return map[req] || (function() { throw new Error("Cannot find module '" + req + "'.") }());
	};
	webpackContext.keys = function webpackContextKeys() {
		return Object.keys(map);
	};
	webpackContext.resolve = webpackContextResolve;
	module.exports = webpackContext;
	webpackContext.id = 82;


/***/ },
/* 83 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "data/TranscomOntology.json?479f03ce4023f1cd8e0dc88ff6eb0a49";

/***/ },
/* 84 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "data/TranscomOntologySmall.json?0b1fe41c8abcbf7f56004de98b1f7682";

/***/ },
/* 85 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "data/benchmark.json?169be55ae232739f0891259af14cbc66";

/***/ },
/* 86 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "data/foaf.json?c534ae8dd245e2bd2c3f2c40995e2e7b";

/***/ },
/* 87 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "data/goodrelations.json?0d0d9d75c255cce24bf095b4fd05d705";

/***/ },
/* 88 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "data/muto.json?dea5ed3ab0ba2b5a531fdc3582a58dc8";

/***/ },
/* 89 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "data/ontovibe.json?80e343a39fa9ad3941cb9adc7ccd51b3";

/***/ },
/* 90 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "data/personasonto.json?5ca3dc1253468891b4dd125f30cfe4f2";

/***/ },
/* 91 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "data/sioc.json?81fa1e1c9687c388934cc0cedefa00da";

/***/ },
/* 92 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__.p + "data/template.json?f3438a62687f74bf3601a060fdbe31fa";

/***/ }
/******/ ]);
//# sourceMappingURL=webvowl.app.js.map