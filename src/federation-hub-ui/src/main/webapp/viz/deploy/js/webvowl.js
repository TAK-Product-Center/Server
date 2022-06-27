/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
webvowl =
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

	__webpack_require__(1);

	var nodeMap = __webpack_require__(5)();
	var propertyMap = __webpack_require__(33)();


	var webvowl = {};
	webvowl.graph = __webpack_require__(47);
	webvowl.options = __webpack_require__(55);
	webvowl.version = "0.5.2";

	webvowl.util = {};
	webvowl.util.constants = __webpack_require__(12);
	webvowl.util.languageTools = __webpack_require__(11);
	webvowl.util.elementTools = __webpack_require__(54);

	webvowl.modules = {};
	webvowl.modules.compactNotationSwitch = __webpack_require__(58);
	webvowl.modules.datatypeFilter = __webpack_require__(59);
	webvowl.modules.disjointFilter = __webpack_require__(61);
	webvowl.modules.focuser = __webpack_require__(62);
	webvowl.modules.nodeDegreeFilter = __webpack_require__(63);
	webvowl.modules.nodeScalingSwitch = __webpack_require__(64);
	webvowl.modules.pickAndPin = __webpack_require__(65);
	webvowl.modules.selectionDetailsDisplayer = __webpack_require__(66);
	webvowl.modules.setOperatorFilter = __webpack_require__(67);
	webvowl.modules.statistics = __webpack_require__(68);
	webvowl.modules.subclassFilter = __webpack_require__(69);


	webvowl.nodes = {};
	nodeMap.entries().forEach(function (entry) {
		mapEntryToIdentifier(webvowl.nodes, entry);
	});

	webvowl.properties = {};
	propertyMap.entries().forEach(function (entry) {
		mapEntryToIdentifier(webvowl.properties, entry);
	});

	function mapEntryToIdentifier(map, entry) {
		var identifier = entry.key.replace(":", "").toLowerCase();
		map[identifier] = entry.value;
	}


	module.exports = webvowl;


/***/ },
/* 1 */
/***/ function(module, exports) {

	// removed by extract-text-webpack-plugin

/***/ },
/* 2 */,
/* 3 */,
/* 4 */,
/* 5 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {var nodes = [];
	nodes.push(__webpack_require__(7));
	nodes.push(__webpack_require__(17));
	nodes.push(__webpack_require__(18));
	nodes.push(__webpack_require__(20));
	nodes.push(__webpack_require__(21));
	nodes.push(__webpack_require__(22));
	nodes.push(__webpack_require__(23));
	nodes.push(__webpack_require__(24));
	nodes.push(__webpack_require__(25));
	nodes.push(__webpack_require__(26));
	nodes.push(__webpack_require__(27));
	nodes.push(__webpack_require__(31));
	nodes.push(__webpack_require__(32));

	var map = d3.map(nodes, function (Prototype) {
		return new Prototype().type();
	});

	module.exports = function () {
		return map;
	};

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 6 */
/***/ function(module, exports) {

	module.exports = d3;

/***/ },
/* 7 */
/***/ function(module, exports, __webpack_require__) {

	var RoundNode = __webpack_require__(8);

	module.exports = (function () {

		var o = function (graph) {
			RoundNode.apply(this, arguments);

			this.attributes(["external"])
				.type("ExternalClass");
		};
		o.prototype = Object.create(RoundNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 8 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {var BaseNode = __webpack_require__(9);
	var drawTools = __webpack_require__(14)();

	module.exports = (function () {

		var o = function (graph) {
			BaseNode.apply(this, arguments);

			var that = this,
				collapsible = false,
				radius = 50,
				collapsingGroupElement,
				pinGroupElement,
				textBlock;


			// Properties
			this.collapsible = function (p) {
				if (!arguments.length) return collapsible;
				collapsible = p;
				return this;
			};

			this.textBlock = function (p) {
				if (!arguments.length) return textBlock;
				textBlock = p;
				return this;
			};

			/**
			 * This might not be equal to the actual radius, because the instance count is used for its calculation.
			 * @param p
			 * @returns {*}
			 */
			this.radius = function (p) {
				if (!arguments.length) return radius;
				radius = p;
				return this;
			};


			// Functions
			this.setHoverHighlighting = function (enable) {
				that.nodeElement().selectAll("circle").classed("hovered", enable);
			};

			this.textWidth = function () {
				return this.actualRadius() * 2;
			};

			this.toggleFocus = function () {
				that.focused(!that.focused());
				that.nodeElement().select("circle").classed("focused", that.focused());
			};

			this.actualRadius = function () {
				if (!graph.options().scaleNodesByIndividuals() || that.individuals().length <= 0) {
					return that.radius();
				} else {
					// we could "listen" for radius and maxIndividualCount changes, but this is easier
					var MULTIPLIER = 8,
						additionalRadius = Math.log(that.individuals().length + 1) * MULTIPLIER + 5;

					return that.radius() + additionalRadius;
				}
			};

			this.distanceToBorder = function () {
				return that.actualRadius();
			};

			/**
			 * Draws the pin on a round node on a position depending on its radius.
			 */
			this.drawPin = function () {
				that.pinned(true);

				pinGroupElement = that.nodeElement()
					.append("g")
					.classed("hidden-in-export", true)
					.attr("transform", function () {
						var dx = (2 / 5) * that.actualRadius(),
							dy = (-7 / 10) * that.actualRadius();
						return "translate(" + dx + "," + dy + ")";
					});

				pinGroupElement.append("circle")
					.classed("class pin feature", true)
					.attr("r", 12)
					.on("click", function () {
						that.removePin();
						d3.event.stopPropagation();
					});

				pinGroupElement.append("line")
					.attr("x1", 0)
					.attr("x2", 0)
					.attr("y1", 12)
					.attr("y2", 16);
			};

			/**
			 * Removes the pin and refreshs the graph to update the force layout.
			 */
			this.removePin = function () {
				that.pinned(false);
				if (pinGroupElement) {
					pinGroupElement.remove();
				}
				graph.updateStyle();
			};

			this.drawCollapsingButton = function () {

				collapsingGroupElement = that.nodeElement()
					.append("g")
					.classed("hidden-in-export", true)
					.attr("transform", function () {
						var dx = (-2 / 5) * that.actualRadius(),
							dy = (1 / 2) * that.actualRadius();
						return "translate(" + dx + "," + dy + ")";
					});

				collapsingGroupElement.append("rect")
					.classed("class pin feature", true)
					.attr("x", 0)
					.attr("y", 0)
					.attr("width", 40)
					.attr("height", 24);

				collapsingGroupElement.append("line")
					.attr("x1", 13)
					.attr("y1", 12)
					.attr("x2", 27)
					.attr("y2", 12);

				collapsingGroupElement.append("line")
					.attr("x1", 20)
					.attr("y1", 6)
					.attr("x2", 20)
					.attr("y2", 18);
			};

			/**
			 * Draws a circular node.
			 * @param parentElement the element to which this node will be appended
			 * @param [additionalCssClasses] additional css classes
			 */
			this.draw = function (parentElement, additionalCssClasses) {
				var cssClasses = that.collectCssClasses();

				that.nodeElement(parentElement);

				if (additionalCssClasses instanceof Array) {
					cssClasses = cssClasses.concat(additionalCssClasses);
				}
				drawTools.appendCircularClass(parentElement, that.actualRadius(), cssClasses, that.labelForCurrentLanguage());

				that.postDrawActions(parentElement);
			};

			/**
			 * Common actions that should be invoked after drawing a node.
			 */
			this.postDrawActions = function () {
				var textBlock = __webpack_require__(15)(that.nodeElement());
				textBlock.addText(that.labelForCurrentLanguage());
				if (!graph.options().compactNotation()) {
					textBlock.addSubText(that.indicationString());
				}
				textBlock.addInstanceCount(that.individuals().length);
				that.textBlock(textBlock);

				that.addMouseListeners();
				if (that.pinned()) {
					that.drawPin();
				}
				if (that.collapsible()) {
					that.drawCollapsingButton();
				}
			};
		};
		o.prototype = Object.create(BaseNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 9 */
/***/ function(module, exports, __webpack_require__) {

	var BaseElement = __webpack_require__(10);
	var forceLayoutNodeFunctions = __webpack_require__(13)();

	module.exports = (function () {

		var Base = function (graph) {
			BaseElement.apply(this, arguments);

			var that = this,
			// Basic attributes
				complement,
				disjointWith,
				individuals = [],
				intersection,
				union,
			// Additional attributes
				maxIndividualCount,
			// Element containers
				nodeElement;


			// Properties
			this.complement = function (p) {
				if (!arguments.length) return complement;
				complement = p;
				return this;
			};

			this.disjointWith = function (p) {
				if (!arguments.length) return disjointWith;
				disjointWith = p;
				return this;
			};

			this.individuals = function (p) {
				if (!arguments.length) return individuals;
				individuals = p || [];
				return this;
			};

			this.intersection = function (p) {
				if (!arguments.length) return intersection;
				intersection = p;
				return this;
			};

			this.maxIndividualCount = function (p) {
				if (!arguments.length) return maxIndividualCount;
				maxIndividualCount = p;
				return this;
			};

			this.nodeElement = function (p) {
				if (!arguments.length) return nodeElement;
				nodeElement = p;
				return this;
			};

			this.union = function (p) {
				if (!arguments.length) return union;
				union = p;
				return this;
			};


			/**
			 * Returns css classes generated from the data of this object.
			 * @returns {Array}
			 */
			that.collectCssClasses = function () {
				var cssClasses = [];

				if (typeof that.styleClass() === "string") {
					cssClasses.push(that.styleClass());
				}

				if (typeof that.visualAttribute() === "string") {
					cssClasses.push(that.visualAttribute());
				}

				return cssClasses;
			};


			// Reused functions TODO refactor
			this.addMouseListeners = function () {
				// Empty node
				if (!that.nodeElement()) {
					console.warn(this);
					return;
				}

				that.nodeElement().selectAll("*")
					.on("mouseover", onMouseOver)
					.on("mouseout", onMouseOut);
			};

			function onMouseOver() {
				if (that.mouseEntered()) {
					return;
				}

				var selectedNode = that.nodeElement().node(),
					nodeContainer = selectedNode.parentNode;

				// Append hovered element as last child to the container list.
				nodeContainer.appendChild(selectedNode);

				that.setHoverHighlighting(true);

				that.mouseEntered(true);
			}

			function onMouseOut() {
				that.setHoverHighlighting(false);

				that.mouseEntered(false);
			}


			forceLayoutNodeFunctions.addTo(this);
		};

		Base.prototype = Object.create(BaseElement.prototype);
		Base.prototype.constructor = Base;


		return Base;
	}());


/***/ },
/* 10 */
/***/ function(module, exports, __webpack_require__) {

	/**
	 * The base element for all visual elements of webvowl.
	 */
	module.exports = (function () {

		var DEFAULT_LABEL = "DEFAULT_LABEL";

		var Base = function (graph) {
			// Basic attributes
			var equivalents = [],
				id,
				label,
				type,
				iri,
				links,
			// Additional attributes
				annotations,
				attributes = [],
				visualAttribute,
				comment,
				description,
				equivalentBase,
			// Style attributes
				focused = false,
				indications = [],
				mouseEntered = false,
				styleClass,
				visible = true,
			// Other
				languageTools = __webpack_require__(11)();


			// Properties
			this.attributes = function (p) {
				if (!arguments.length) return attributes;
				attributes = p;
				return this;
			};

			this.annotations = function (p) {
				if (!arguments.length) return annotations;
				annotations = p;
				return this;
			};

			this.comment = function (p) {
				if (!arguments.length) return comment;
				comment = p;
				return this;
			};

			this.description = function (p) {
				if (!arguments.length) return description;
				description = p;
				return this;
			};

			this.equivalents = function (p) {
				if (!arguments.length) return equivalents;
				equivalents = p || [];
				return this;
			};

			this.equivalentBase = function (p) {
				if (!arguments.length) return equivalentBase;
				equivalentBase = p;
				return this;
			};

			this.focused = function (p) {
				if (!arguments.length) return focused;
				focused = p;
				return this;
			};

			this.id = function (p) {
				if (!arguments.length) return id;
				id = p;
				return this;
			};

			this.indications = function (p) {
				if (!arguments.length) return indications;
				indications = p;
				return this;
			};

			this.iri = function (p) {
				if (!arguments.length) return iri;
				iri = p;
				return this;
			};

			this.label = function (p) {
				if (!arguments.length) return label;
				label = p || DEFAULT_LABEL;
				return this;
			};

			this.links = function (p) {
				if (!arguments.length) return links;
				links = p;
				return this;
			};

			this.mouseEntered = function (p) {
				if (!arguments.length) return mouseEntered;
				mouseEntered = p;
				return this;
			};

			this.styleClass = function (p) {
				if (!arguments.length) return styleClass;
				styleClass = p;
				return this;
			};

			this.type = function (p) {
				if (!arguments.length) return type;
				type = p;
				return this;
			};

			this.visible = function (p) {
				if (!arguments.length) return visible;
				visible = p;
				return this;
			};

			this.visualAttribute = function (p) {
				if (!arguments.length) return visualAttribute;
				visualAttribute = p;
				return this;
			};


			this.commentForCurrentLanguage = function () {
				return languageTools.textInLanguage(this.comment(), graph.language());
			};

			/**
			 * @returns {string} the css class of this node..
			 */
			this.cssClassOfNode = function () {
				return "node" + this.id();
			};

			this.descriptionForCurrentLanguage = function () {
				return languageTools.textInLanguage(this.description(), graph.language());
			};

			this.defaultLabel = function () {
				return languageTools.textInLanguage(this.label(), "default");
			};

			this.indicationString = function () {
				return this.indications().join(", ");
			};

			this.labelForCurrentLanguage = function () {
				return languageTools.textInLanguage(this.label(), graph.language());
			};
		};

		Base.prototype.constructor = Base;

		Base.prototype.equals = function(other) {
			return other instanceof Base && this.id() === other.id();
		};


		return Base;
	}());


/***/ },
/* 11 */
/***/ function(module, exports, __webpack_require__) {

	var constants = __webpack_require__(12)();

	/**
	 * Encapsulates methods which return a label in a specific language for a preferred language.
	 */
	module.exports = (function () {

		var languageTools = {};


		languageTools.textInLanguage = function (textObject, preferredLanguage) {
			if (typeof textObject === "undefined") {
				return undefined;
			}

			if (typeof textObject === "string") {
				return textObject;
			}

			if (preferredLanguage && textObject.hasOwnProperty(preferredLanguage)) {
				return textObject[preferredLanguage];
			}

			var textForLanguage = searchLanguage(textObject, "en");
			if (textForLanguage) {
				return textForLanguage;
			}
			textForLanguage = searchLanguage(textObject, constants.LANG_UNDEFINED);
			if (textForLanguage) {
				return textForLanguage;
			}

			return textObject[constants.LANG_IRIBASED];
		};


		function searchLanguage(textObject, preferredLanguage) {
			for (var language in textObject) {
				if (language === preferredLanguage && textObject.hasOwnProperty(language)) {
					return textObject[language];
				}
			}
		}

		return function () {
			/* Use a function here to keep a consistent style like webvowl.path.to.module()
			 * despite having just a single languageTools object. */
			return languageTools;
		};
	})();


/***/ },
/* 12 */
/***/ function(module, exports) {

	module.exports = (function () {

		var constants = {};

		constants.LANG_IRIBASED = "IRI-based";
		constants.LANG_UNDEFINED = "undefined";

		return function () {
			/* Use a function here to keep a consistent style like webvowl.path.to.module()
			 * despite having just a single object. */
			return constants;
		};
	})();


/***/ },
/* 13 */
/***/ function(module, exports) {

	/**
	 * The functions for controlling attributes of nodes of the force layout can't be modelled to the element hierarchy,
	 * which is used for inheriting visual and OWL-like attributes.
	 *
	 * To reduce code redundancy the common functions for controlling the force layout node attributes are excluded into this
	 * module, which can add them to the node objects.
	 *
	 * @type {{}}
	 */
	var nodeFunctions = {};
	module.exports = function () {
		return nodeFunctions;
	};


	nodeFunctions.addTo = function (node) {
		addFixedLocationFunctions(node);
	};

	function addFixedLocationFunctions(node) {
		var locked = false,
			frozen = false,
			pinned = false;

		node.locked = function (p) {
			if (!arguments.length) {
				return locked;
			}
			locked = p;
			applyFixedLocationAttributes();
			return node;
		};

		node.frozen = function (p) {
			if (!arguments.length) {
				return frozen;
			}
			frozen = p;
			applyFixedLocationAttributes();
			return node;
		};

		node.pinned = function (p) {
			if (!arguments.length) {
				return pinned;
			}
			pinned = p;
			applyFixedLocationAttributes();
			return node;
		};

		function applyFixedLocationAttributes() {
			if (node.locked() || node.frozen() || node.pinned()) {
				node.fixed = true;
			} else {
				node.fixed = false;
			}
		}
	}


/***/ },
/* 14 */
/***/ function(module, exports) {

	/**
	 * Contains reusable function for drawing nodes.
	 */
	module.exports = (function () {

		var tools = {};

		/**
		 * Append a circular class node with the passed attributes.
		 * @param parent the parent element to which the circle will be appended
		 * @param radius
		 * @param cssClasses an array of additional css classes
		 * @param [tooltip]
		 * @returns {*}
		 */
		tools.appendCircularClass = function (parent, radius, cssClasses, tooltip) {
			var circle = parent.append("circle")
				.classed("class", true)
				.attr("r", radius);

			addCssClasses(circle, cssClasses);
			addToolTip(circle, tooltip);

			return circle;
		};

		function addCssClasses(element, cssClasses) {
			if (cssClasses instanceof Array) {
				cssClasses.forEach(function (cssClass) {
					element.classed(cssClass, true);
				});
			}
		}

		function addToolTip(element, tooltip) {
			if (tooltip) {
				element.append("title").text(tooltip);
			}
		}

		/**
		 * Appends a rectangular class node with the passed attributes.
		 * @param parent the parent element to which the rectangle will be appended
		 * @param width
		 * @param height
		 * @param cssClasses an array of additional css classes
		 * @param [tooltip]
		 * @returns {*}
		 */
		tools.appendRectangularClass = function (parent, width, height, cssClasses, tooltip) {
			var rectangle = parent.append("rect")
				.classed("class", true)
				.attr("x", -width / 2)
				.attr("y", -height / 2)
				.attr("width", width)
				.attr("height", height);

			addCssClasses(rectangle, cssClasses);
			addToolTip(rectangle, tooltip);

			return rectangle;
		};


		return function () {
			// Encapsulate into function to maintain default.module.path()
			return tools;
		};
	})();


/***/ },
/* 15 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {var textTools = __webpack_require__(16)();

	/**
	 * Creates a new textblock in the specified element.
	 * @param element The element/group where the text block should be appended.
	 * @constructor New text block where additional <tspan>'s can be applied to.
	 */
	module.exports = function (element) {

		var textElement = {},
			LINE_DISTANCE = 1,
			SUBTEXT_CSS_CLASS = "subtext",
			textBlock = element.append("text")
				.classed("text", true)
				.attr("text-anchor", "middle");

		/**
		 * Repositions the textblock according to its own offsetHeight.
		 */
		function repositionTextBlock() {
			// Nothing to do if no child elements exist
			var lineCount = getLineCount();
			if (lineCount < 1) {
				textBlock.attr("y", 0);
				return;
			}

			var textBlockHeight = getTextBlockHeight(textBlock);
			textBlock.attr("y", -textBlockHeight * 0.6 + "px");
		}

		/**
		 * Adds a new line of text to the element.
		 * @param text
		 */
		textElement.addText = function (text) {
			addTextline(text);
		};

		/**
		 * Adds a line of text in subproperty style.
		 * @param text
		 */
		textElement.addSubText = function (text) {
			addTextline(text, SUBTEXT_CSS_CLASS, "(", ")");
		};

		/**
		 * Adds a line of text in equivalent node listing style.
		 * @param text
		 */
		textElement.addEquivalents = function (text) {
			addTextline(text, SUBTEXT_CSS_CLASS, "[", "]");
		};

		/**
		 * Adds a label with the instance count.
		 * @param instanceCount
		 */
		textElement.addInstanceCount = function (instanceCount) {
			if (instanceCount) {
				addTextline(instanceCount.toString(), "instance-count");
			}
		};

		function getLineCount() {
			return textBlock.property("childElementCount") - textBlock.selectAll(".instance-count").size();
		}

		function addTextline(text, subtextCssClass, prefix, postfix) {
			if (!text) {
				return;
			}

			var truncatedText, tspan;

			subtextCssClass = subtextCssClass || "text";
			truncatedText = textTools.truncate(text, element.datum().textWidth(), subtextCssClass);

			tspan = textBlock.append("tspan")
				.classed("text", true)
				.classed(subtextCssClass, true)
				.text(applyPreAndPostFix(truncatedText, prefix, postfix))
				.attr("x", 0)
				.attr("dy", function () {
					var heightInPixels = getPixelHeightOfTextLine(d3.select(this)),
						siblingCount = getLineCount() - 1,
						lineDistance = siblingCount > 0 ? LINE_DISTANCE : 0;
					return heightInPixels + lineDistance + "px";
				});

			repositionTextBlock();
		}

		function applyPreAndPostFix(text, prefix, postfix) {
			if (prefix) {
				text = prefix + text;
			}
			if (postfix) {
				text += postfix;
			}
			return text;
		}

		function getPixelHeightOfTextLine(textElement) {
			/* Due to browser incompatibilities this has to be hardcoded. This is because Firefox has no
			 * "offsetHeight" attribute like Chrome to retrieve the absolute pixel height. */
			if (textElement.classed("subtext")) {
				return 10;
			} else {
				return 14;
			}
		}

		function getTextBlockHeight(textBlock) {
			/* Hardcoded due to the same reasons like in the getPixelHeightOfTextLine function. */

			var children = textBlock.selectAll("*"),
				childCount = children.size();
			if (childCount === 0) {
				return 0;
			}

			// Values retrieved by testing
			var pixelHeight = childCount * LINE_DISTANCE;
			children.each(function () {
				pixelHeight += getPixelHeightOfTextLine(d3.select(this));
			});

			return pixelHeight;
		}

		textElement.setTranslation = function (x, y) {
			textBlock.attr("transform", "translate(" + x + ", " + y + ")");
		};

		textElement.clear = function () {
			textBlock.selectAll("*").remove();
		};

		return textElement;
	};

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 16 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {var ADDITIONAL_TEXT_SPACE = 4;

	var tools = {};

	function measureTextWidth(text, textStyle) {
		// Set a default value
		if (!textStyle) {
			textStyle = "text";
		}
		var d = d3.select("body")
				.append("div")
				.attr("class", textStyle)
				.attr("id", "width-test") // tag this element to identify it
				.attr("style", "position:absolute; float:left; white-space:nowrap; visibility:hidden;")
				.text(text),
			w = document.getElementById("width-test").offsetWidth;
		d.remove();
		return w;
	}

	tools.truncate = function (text, maxWidth, textStyle, additionalTextSpace) {
		maxWidth -= isNaN(additionalTextSpace) ? ADDITIONAL_TEXT_SPACE : additionalTextSpace;
		if (isNaN(maxWidth) || maxWidth <= 0) {
			return text;
		}

		var truncatedText = text,
			newTruncatedTextLength,
			textWidth,
			ratio;

		while (true) {
			textWidth = measureTextWidth(truncatedText, textStyle);
			if (textWidth <= maxWidth) {
				break;
			}

			ratio = textWidth / maxWidth;
			newTruncatedTextLength = Math.floor(truncatedText.length / ratio);

			// detect if nothing changes
			if (truncatedText.length === newTruncatedTextLength) {
				break;
			}

			truncatedText = truncatedText.substring(0, newTruncatedTextLength);
		}

		if (text.length > truncatedText.length) {
			return text.substring(0, truncatedText.length - 3) + "...";
		}
		return text;
	};


	module.exports = function () {
		return tools;
	};

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 17 */
/***/ function(module, exports, __webpack_require__) {

	var RoundNode = __webpack_require__(8);

	module.exports = (function () {

		var o = function (graph) {
			RoundNode.apply(this, arguments);

			this.type("owl:Class");
		};
		o.prototype = Object.create(RoundNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 18 */
/***/ function(module, exports, __webpack_require__) {

	var SetOperatorNode = __webpack_require__(19);

	module.exports = (function () {

		var o = function (graph) {
			SetOperatorNode.apply(this, arguments);

			var that = this;

			this.styleClass("complementof")
				.type("owl:complementOf");

			this.draw = function (element) {
				that.nodeElement(element);

				element.append("circle")
					.attr("class", that.type())
					.classed("class", true)
					.classed("special", true)
					.attr("r", that.actualRadius());

				var symbol = element.append("g").classed("embedded", true);

				symbol.append("circle")
					.attr("class", "symbol")
					.classed("fineline", true)
					.attr("r", (that.radius() - 15));
				symbol.append("path")
					.attr("class", "nofill")
					.attr("d", "m -7,-1.5 12,0 0,6");

				symbol.attr("transform", "translate(-" + (that.radius() - 15) / 100 + ",-" + (that.radius() - 15) / 100 + ")");

				that.postDrawActions();
			};
		};
		o.prototype = Object.create(SetOperatorNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 19 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {var RoundNode = __webpack_require__(8);

	module.exports = (function () {

		var radius = 40;

		var o = function (graph) {
			RoundNode.apply(this, arguments);

			var that = this,
				superHoverHighlightingFunction = this.setHoverHighlighting,
				superPostDrawActions = this.postDrawActions;

			this.radius(radius);

			this.setHoverHighlighting = function (enable) {
				superHoverHighlightingFunction(enable);

				d3.selectAll(".special." + that.cssClassOfNode()).classed("hovered", enable);
			};

			this.postDrawActions = function () {
				superPostDrawActions();

				that.textBlock().clear();
				that.textBlock().addInstanceCount(that.individuals().length);
				that.textBlock().setTranslation(0, that.radius() - 15);
			};
		};
		o.prototype = Object.create(RoundNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 20 */
/***/ function(module, exports, __webpack_require__) {

	var RoundNode = __webpack_require__(8);

	module.exports = (function () {

		var o = function (graph) {
			RoundNode.apply(this, arguments);

			this.attributes(["deprecated"])
				.type("owl:DeprecatedClass");
		};
		o.prototype = Object.create(RoundNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 21 */
/***/ function(module, exports, __webpack_require__) {

	var RoundNode = __webpack_require__(8);
	var drawTools = __webpack_require__(14)();

	module.exports = (function () {

		var o = function (graph) {
			RoundNode.apply(this, arguments);

			var CIRCLE_SIZE_DIFFERENCE = 4;

			var that = this,
				superActualRadiusFunction = that.actualRadius;

			this.styleClass("equivalentclass")
				.type("owl:equivalentClass");

			this.actualRadius = function () {
				return superActualRadiusFunction() + CIRCLE_SIZE_DIFFERENCE;
			};


			this.draw = function (parentElement) {
				var cssClasses = that.collectCssClasses();

				that.nodeElement(parentElement);

				// draw the outer circle at first and afterwards the inner circle
				drawTools.appendCircularClass(parentElement, that.actualRadius(), ["white", "embedded"]);
				drawTools.appendCircularClass(parentElement, that.actualRadius() - CIRCLE_SIZE_DIFFERENCE, cssClasses, that.labelForCurrentLanguage());

				that.postDrawActions();
				appendEquivalentClasses(that.textBlock(), that.equivalents());
			};

			function appendEquivalentClasses(textBlock, equivalentClasses) {
				if (typeof equivalentClasses === "undefined") {
					return;
				}

				var equivalentNames,
					equivalentNamesString;

				equivalentNames = equivalentClasses.map(function (node) {
					return node.labelForCurrentLanguage();
				});
				equivalentNamesString = equivalentNames.join(", ");

				textBlock.addEquivalents(equivalentNamesString);
			}

			/**
			 * Sets the hover highlighting of this node.
			 * @param enable
			 */
			that.setHoverHighlighting = function (enable) {
				that.nodeElement().selectAll("circle:last-of-type").classed("hovered", enable);
			};
		};
		o.prototype = Object.create(RoundNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 22 */
/***/ function(module, exports, __webpack_require__) {

	var SetOperatorNode = __webpack_require__(19);

	module.exports = (function () {

		var o = function (graph) {
			SetOperatorNode.apply(this, arguments);

			var that = this;

			this.styleClass("intersectionof")
				.type("owl:intersectionOf");

			this.draw = function (element) {
				that.nodeElement(element);

				element.append("circle")
					.attr("class", that.type())
					.classed("class", true)
					.classed("special", true)
					.attr("r", that.actualRadius());

				var symbol = element.append("g").classed("embedded", true);

				symbol.append("path")
					.attr("class", "nostroke")
					.classed("symbol", true).attr("d", "m 24.777,0.771 c0,16.387-13.607,23.435-19.191,23.832S-15.467," +
						"14.526-15.467,0.424S-1.216-24.4,5.437-24.4 C12.09-24.4,24.777-15.616,24.777,0.771z");
				symbol.append("circle")
					.attr("class", "nofill")
					.classed("fineline", true)
					.attr("r", (that.radius() - 15));
				symbol.append("circle")
					.attr("cx", 10)
					.attr("class", "nofill")
					.classed("fineline", true)
					.attr("r", (that.radius() - 15));
				symbol.append("path")
					.attr("class", "nofill")
					.attr("d", "m 9,5 c 0,-2 0,-4 0,-6 0,0 0,0 0,0 0,0 0,-1.8 -1,-2.3 -0.7,-0.6 -1.7,-0.8 -2.9," +
							   "-0.8 -1.2,0 -2,0 -3,0.8 -0.7,0.5 -1,1.4 -1,2.3 0,2 0,4 0,6");

				symbol.attr("transform", "translate(-" + (that.radius() - 15) / 5 + ",-" + (that.radius() - 15) / 100 + ")");

				that.postDrawActions();
			};
		};
		o.prototype = Object.create(SetOperatorNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 23 */
/***/ function(module, exports, __webpack_require__) {

	var OwlThing = __webpack_require__(24);

	module.exports = (function () {

		var o = function (graph) {
			OwlThing.apply(this, arguments);

			this.label("Nothing")
				.type("owl:Nothing")
				.iri("http://www.w3.org/2002/07/owl#Nothing");
		};
		o.prototype = Object.create(OwlThing.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 24 */
/***/ function(module, exports, __webpack_require__) {

	var RoundNode = __webpack_require__(8);

	module.exports = (function () {

		var o = function (graph) {
			RoundNode.apply(this, arguments);

			var superDrawFunction = this.draw;

			this.label("Thing")
				.type("owl:Thing")
				.iri("http://www.w3.org/2002/07/owl#Thing")
				.radius(30);

			this.draw = function (element) {
				superDrawFunction(element, ["white", "special"]);
			};
		};
		o.prototype = Object.create(RoundNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 25 */
/***/ function(module, exports, __webpack_require__) {

	var SetOperatorNode = __webpack_require__(19);

	module.exports = (function () {

		var o = function (graph) {
			SetOperatorNode.apply(this, arguments);

			var that = this;

			this.styleClass("unionof")
				.type("owl:unionOf");

			this.draw = function (element) {
				that.nodeElement(element);

				element.append("circle")
					.attr("class", that.type())
					.classed("class", true)
					.classed("special", true)
					.attr("r", that.actualRadius());

				var symbol = element.append("g").classed("embedded", true);

				symbol.append("circle")
					.attr("class", "symbol")
					.attr("r", (that.radius() - 15));
				symbol.append("circle")
					.attr("cx", 10)
					.attr("class", "symbol")
					.classed("fineline", true)
					.attr("r", (that.radius() - 15));
				symbol.append("circle")
					.attr("class", "nofill")
					.classed("fineline", true)
					.attr("r", (that.radius() - 15));
				symbol.append("path")
					.attr("class", "link")
					.attr("d", "m 1,-3 c 0,2 0,4 0,6 0,0 0,0 0,0 0,2 2,3 4,3 2,0 4,-1 4,-3 0,-2 0,-4 0,-6");

				symbol.attr("transform", "translate(-" + (that.radius() - 15) / 5 + ",-" + (that.radius() - 15) / 100 + ")");

				that.postDrawActions();
			};
		};
		o.prototype = Object.create(SetOperatorNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 26 */
/***/ function(module, exports, __webpack_require__) {

	var RoundNode = __webpack_require__(8);

	module.exports = (function () {

		var o = function (graph) {
			RoundNode.apply(this, arguments);

			this.attributes(["rdf"])
				.type("rdfs:Class");
		};
		o.prototype = Object.create(RoundNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 27 */
/***/ function(module, exports, __webpack_require__) {

	var DatatypeNode = __webpack_require__(28);

	module.exports = (function () {

		var o = function (graph) {
			DatatypeNode.apply(this, arguments);

			this.attributes(["datatype"])
				.type("rdfs:Datatype");
		};
		o.prototype = Object.create(DatatypeNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 28 */
/***/ function(module, exports, __webpack_require__) {

	var RectangularNode = __webpack_require__(29);

	module.exports = (function () {

		var o = function (graph) {
			RectangularNode.apply(this, arguments);
		};
		o.prototype = Object.create(RectangularNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 29 */
/***/ function(module, exports, __webpack_require__) {

	var BaseNode = __webpack_require__(9);
	var drawTools = __webpack_require__(14)();
	var rectangularElementTools = __webpack_require__(30)();

	module.exports = (function () {

		var o = function (graph) {
			BaseNode.apply(this, arguments);

			var that = this,
				height = 20,
				width = 60,
				smallestRadius = height / 2;


			// Properties
			this.height = function (p) {
				if (!arguments.length) return height;
				height = p;
				return this;
			};

			this.width = function (p) {
				if (!arguments.length) return width;
				width = p;
				return this;
			};


			// Functions
			// for compatibility reasons // TODO resolve
			this.actualRadius = function () {
				return smallestRadius;
			};

			this.distanceToBorder = function (dx, dy) {
				return rectangularElementTools.distanceToBorder(that, dx, dy);
			};

			this.setHoverHighlighting = function (enable) {
				that.nodeElement().selectAll("rect").classed("hovered", enable);
			};

			this.textWidth = function () {
				return this.width();
			};

			this.toggleFocus = function () {
				that.focused(!that.focused());
				that.nodeElement().select("rect").classed("focused", that.focused());
			};

			/**
			 * Draws the rectangular node.
			 * @param parentElement the element to which this node will be appended
			 * @param [additionalCssClasses] additional css classes
			 */
			this.draw = function (parentElement, additionalCssClasses) {
				var textBlock,
					cssClasses = that.collectCssClasses();

				that.nodeElement(parentElement);

				if (additionalCssClasses instanceof Array) {
					cssClasses = cssClasses.concat(additionalCssClasses);
				}
				drawTools.appendRectangularClass(parentElement, that.width(), that.height(), cssClasses, that.labelForCurrentLanguage());

				textBlock = __webpack_require__(15)(parentElement);
				textBlock.addText(that.labelForCurrentLanguage());

				that.addMouseListeners();
			};
		};
		o.prototype = Object.create(BaseNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 30 */
/***/ function(module, exports) {

	var tools = {};
	module.exports = function () {
		return tools;
	};

	tools.distanceToBorder = function (rect, dx, dy) {
		var width = rect.width(),
			height = rect.height();

		var innerDistance,
			m_link = Math.abs(dy / dx),
			m_rect = height / width;

		if (m_link <= m_rect) {
			var timesX = dx / (width / 2),
				rectY = dy / timesX;
			innerDistance = Math.sqrt(Math.pow(width / 2, 2) + Math.pow(rectY, 2));
		} else {
			var timesY = dy / (height / 2),
				rectX = dx / timesY;
			innerDistance = Math.sqrt(Math.pow(height / 2, 2) + Math.pow(rectX, 2));
		}

		return innerDistance;
	};


/***/ },
/* 31 */
/***/ function(module, exports, __webpack_require__) {

	var DatatypeNode = __webpack_require__(28);

	module.exports = (function () {

		var o = function (graph) {
			DatatypeNode.apply(this, arguments);

			var superDrawFunction = this.draw,
				superLabelFunction = this.label;

			this.attributes(["datatype"])
				.label("Literal")
				.styleClass("literal")
				.type("rdfs:Literal")
				.iri("http://www.w3.org/2000/01/rdf-schema#Literal");

			this.draw = function (element) {
				superDrawFunction(element, ["special"]);
			};

			this.label = function (p) {
				if (!arguments.length) return superLabelFunction();
				return this;
			};
		};
		o.prototype = Object.create(DatatypeNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 32 */
/***/ function(module, exports, __webpack_require__) {

	var RoundNode = __webpack_require__(8);

	module.exports = (function () {

		var o = function (graph) {
			RoundNode.apply(this, arguments);

			var superDrawFunction = this.draw;

			this.attributes(["rdf"])
				.label("Resource")
				.radius(30)
				.styleClass("rdfsresource")
				.type("rdfs:Resource");

			this.draw = function (element) {
				superDrawFunction(element, ["rdf", "special"]);
			};
		};
		o.prototype = Object.create(RoundNode.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 33 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {var properties = [];
	properties.push(__webpack_require__(34));
	properties.push(__webpack_require__(37));
	properties.push(__webpack_require__(36));
	properties.push(__webpack_require__(38));
	properties.push(__webpack_require__(39));
	properties.push(__webpack_require__(40));
	properties.push(__webpack_require__(41));
	properties.push(__webpack_require__(42));
	properties.push(__webpack_require__(43));
	properties.push(__webpack_require__(44));
	properties.push(__webpack_require__(45));
	properties.push(__webpack_require__(46));

	var map = d3.map(properties, function (Prototype) {
		return new Prototype().type();
	});

	module.exports = function () {
		return map;
	};

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 34 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);

	module.exports = (function () {

		var o = function (graph) {
			BaseProperty.apply(this, arguments);

			this.attributes(["datatype"])
				.styleClass("datatypeproperty")
				.type("owl:DatatypeProperty");
		};
		o.prototype = Object.create(BaseProperty.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 35 */
/***/ function(module, exports, __webpack_require__) {

	var BaseElement = __webpack_require__(10);
	var rectangularElementTools = __webpack_require__(30)();

	module.exports = (function () {

		// Static variables
		var labelHeight = 28,
			labelWidth = 80,
			smallestRadius = labelHeight / 2;


		// Constructor, private variables and privileged methods
		var Base = function (graph) {
			BaseElement.apply(this, arguments);

			var that = this,
			// Basic attributes
				cardinality,
				domain,
				inverse,
				link,
				minCardinality,
				maxCardinality,
				range,
				subproperties,
				superproperties,
			// Style attributes
				linkType = "normal",
				markerType = "normal",
				labelVisible = true,
			// Element containers
				cardinalityElement,
				labelElement,
				linkGroup,
				markerElement,
			// Other
				redundantProperties = [];


			// Properties
			this.cardinality = function (p) {
				if (!arguments.length) return cardinality;
				cardinality = p;
				return this;
			};

			this.cardinalityElement = function (p) {
				if (!arguments.length) return cardinalityElement;
				cardinalityElement = p;
				return this;
			};

			this.domain = function (p) {
				if (!arguments.length) return domain;
				domain = p;
				return this;
			};

			this.inverse = function (p) {
				if (!arguments.length) return inverse;
				inverse = p;
				return this;
			};

			this.labelElement = function (p) {
				if (!arguments.length) return labelElement;
				labelElement = p;
				return this;
			};

			this.labelVisible = function (p) {
				if (!arguments.length) return labelVisible;
				labelVisible = p;
				return this;
			};

			this.link = function (p) {
				if (!arguments.length) return link;
				link = p;
				return this;
			};

			this.linkGroup = function (p) {
				if (!arguments.length) return linkGroup;
				linkGroup = p;
				return this;
			};

			this.linkType = function (p) {
				if (!arguments.length) return linkType;
				linkType = p;
				return this;
			};

			this.markerElement = function (p) {
				if (!arguments.length) return markerElement;
				markerElement = p;
				return this;
			};

			this.markerType = function (p) {
				if (!arguments.length) return markerType;
				markerType = p;
				return this;
			};

			this.maxCardinality = function (p) {
				if (!arguments.length) return maxCardinality;
				maxCardinality = p;
				return this;
			};

			this.minCardinality = function (p) {
				if (!arguments.length) return minCardinality;
				minCardinality = p;
				return this;
			};

			this.range = function (p) {
				if (!arguments.length) return range;
				range = p;
				return this;
			};

			this.redundantProperties = function (p) {
				if (!arguments.length) return redundantProperties;
				redundantProperties = p;
				return this;
			};

			this.subproperties = function (p) {
				if (!arguments.length) return subproperties;
				subproperties = p;
				return this;
			};

			this.superproperties = function (p) {
				if (!arguments.length) return superproperties;
				superproperties = p;
				return this;
			};


			// Functions
			this.distanceToBorder = function (dx, dy) {
				return rectangularElementTools.distanceToBorder(that, dx, dy);
			};

			this.isSpecialLink = function () {
				return linkType === "special";
			};

			this.markerId = function () {
				return "marker" + that.id();
			};

			this.toggleFocus = function () {
				that.focused(!that.focused());
				labelElement.select("rect").classed("focused", that.focused());
			};


			// Reused functions TODO refactor
			this.draw = function (labelGroup) {
				function attachLabel(property) {
					// Draw the label and its background
					var label = labelGroup.append("g")
						.datum(property)
						.classed("label", true)
						.attr("id", property.id());
					property.addRect(label);

					// Attach the text and perhaps special elements
					var textBox = __webpack_require__(15)(label);
					if (property instanceof __webpack_require__(36)) {
						property.addDisjointLabel(labelGroup, textBox);
						return label;
					} else {
						textBox.addText(property.labelForCurrentLanguage());
					}

					textBox.addSubText(property.indicationString());
					property.addEquivalentsToLabel(textBox);

					return label;
				}

				if (!that.labelVisible()) {
					return undefined;
				}

				that.labelElement(attachLabel(that));

				// Draw an inverse label and reposition both labels if necessary
				if (that.inverse()) {
					var yTransformation = (that.height() / 2) + 1 /* additional space */;
					that.inverse()
						.labelElement(attachLabel(that.inverse()));

					that.labelElement()
						.attr("transform", "translate(" + 0 + ",-" + yTransformation + ")");
					that.inverse()
						.labelElement()
						.attr("transform", "translate(" + 0 + "," + yTransformation + ")");
				}

				return that.labelElement();
			};

			this.addRect = function (groupTag) {
				var rect = groupTag.append("rect")
					.classed(that.styleClass(), true)
					.classed("property", true)
					.attr("x", -that.width() / 2)
					.attr("y", -that.height() / 2)
					.attr("width", that.width())
					.attr("height", that.height())
					.on("mouseover", function () {
						onMouseOver();
					})
					.on("mouseout", function () {
						onMouseOut();
					});

				rect.append("title")
					.text(that.labelForCurrentLanguage());

				if (that.visualAttribute()) {
					rect.classed(that.visualAttribute(), true);
				}
			};
			this.addDisjointLabel = function (groupTag, textTag) {
				groupTag.append("circle")
					.classed("symbol", true)
					.classed("fineline", true)
					.classed("embedded", true)
					.attr("cx", -12.5)
					.attr("r", 10);

				groupTag.append("circle")
					.classed("symbol", true)
					.classed("fineline", true)
					.classed("embedded", true)
					.attr("cx", 12.5)
					.attr("r", 10);

				if (!graph.options().compactNotation()) {
					textTag.addSubText("disjoint");
				}
				textTag.setTranslation(0, 20);
			};
			this.addEquivalentsToLabel = function (textBox) {
				if (that.equivalents()) {
					var equivalentLabels,
						equivalentString;

					equivalentLabels = that.equivalents().map(function (property) {
						return property.labelForCurrentLanguage();
					});
					equivalentString = equivalentLabels.join(", ");

					textBox.addEquivalents(equivalentString);
				}
			};
			this.drawCardinality = function (cardinalityGroup) {
				if (that.minCardinality() === undefined &&
					that.maxCardinality() === undefined &&
					that.cardinality() === undefined) {
					return undefined;
				}

				// Drawing cardinality groups
				that.cardinalityElement(cardinalityGroup.classed("cardinality", true));

				var cardText = cardinalityGroup.append("text")
					.classed("cardinality", true)
					.attr("text-anchor", "middle")
					.attr("dy", "0.5ex");

				if (that.minCardinality() !== undefined) {
					var cardString = that.minCardinality() + "..";
					cardString += that.maxCardinality() !== undefined ? that.maxCardinality() : "*";

					cardText.text(cardString);
				} else if (that.maxCardinality() !== undefined) {
					cardText.text("*.." + that.maxCardinality());
				} else if (that.cardinality() !== undefined) {
					cardText.text(that.cardinality());
				}

				return that.cardinalityElement();
			};
			function onMouseOver() {
				if (that.mouseEntered()) {
					return;
				}
				that.mouseEntered(true);

				setHighlighting(true);

				that.foreground();
				foregroundSubAndSuperProperties();
			}

			function setHighlighting(enable) {
				that.labelElement().select("rect").classed("hovered", enable);
				that.linkGroup().selectAll("path, text").classed("hovered", enable);
				that.markerElement().select("path").classed("hovered", enable);
				if (that.cardinalityElement()) {
					that.cardinalityElement().classed("hovered", enable);
				}

				var subAndSuperProperties = getSubAndSuperProperties();
				subAndSuperProperties.forEach(function (property) {
					property.labelElement().select("rect")
						.classed("indirectHighlighting", enable);
				});
			}

			/**
			 * Combines the sub- and superproperties into a single array, because
			 * they're often used equivalently.
			 * @returns {Array}
			 */
			function getSubAndSuperProperties() {
				var properties = [];

				if (that.subproperties()) {
					properties = properties.concat(that.subproperties());
				}
				if (that.superproperties()) {
					properties = properties.concat(that.superproperties());
				}

				return properties;
			}

			/**
			 * Foregrounds the property, its inverse and the link.
			 */
			this.foreground = function () {
				var selectedLabelGroup = that.labelElement().node().parentNode,
					labelContainer = selectedLabelGroup.parentNode,
					selectedLinkGroup = that.linkGroup().node(),
					linkContainer = that.linkGroup().node().parentNode;

				// Append hovered element as last child to the container list.
				labelContainer.appendChild(selectedLabelGroup);
				linkContainer.appendChild(selectedLinkGroup);
			};

			/**
			 * Foregrounds the sub- and superproperties of this property.
			 * This is separated from the foreground-function to prevent endless loops.
			 */
			function foregroundSubAndSuperProperties() {
				var subAndSuperProperties = getSubAndSuperProperties();

				subAndSuperProperties.forEach(function (property) {
					property.foreground();
				});
			}

			function onMouseOut() {
				that.mouseEntered(false);

				setHighlighting(false);
			}

		};

		Base.prototype = Object.create(BaseElement.prototype);
		Base.prototype.constructor = Base;

		Base.prototype.height = function () {
			return labelHeight;
		};

		Base.prototype.width = function () {
			return labelWidth;
		};

		Base.prototype.actualRadius = function() {
			return smallestRadius;
		};

		Base.prototype.textWidth = Base.prototype.width;


		return Base;
	}());


/***/ },
/* 36 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);

	module.exports = (function () {

		var o = function (graph) {
			BaseProperty.apply(this, arguments);

			var label = "Disjoint With";
			// Disallow overwriting the label
			this.label = function (p) {
				if (!arguments.length) return label;
				return this;
			};

			this.markerType("special")
				.linkType("special")
				.styleClass("disjointwith")
				.type("owl:disjointWith");
		};
		o.prototype = Object.create(BaseProperty.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 37 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);

	module.exports = (function () {

		var o = function (graph) {
			BaseProperty.apply(this, arguments);

			this.attributes(["deprecated"])
				.styleClass("deprecatedproperty")
				.type("owl:DeprecatedProperty");
		};
		o.prototype = Object.create(BaseProperty.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 38 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);

	module.exports = (function () {

		var o = function (graph) {
			BaseProperty.apply(this, arguments);

			this.styleClass("equivalentproperty")
				.type("owl:equivalentProperty");
		};
		o.prototype = Object.create(BaseProperty.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 39 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);

	module.exports = (function () {

		var o = function (graph) {
			BaseProperty.apply(this, arguments);

			this.attributes(["functional"])
				.styleClass("functionalproperty")
				.type("owl:FunctionalProperty");
		};
		o.prototype = Object.create(BaseProperty.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 40 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);

	module.exports = (function () {

		var o = function (graph) {
			BaseProperty.apply(this, arguments);

			this.attributes(["inverse functional"])
				.styleClass("inversefunctionalproperty")
				.type("owl:InverseFunctionalProperty");
		};
		o.prototype = Object.create(BaseProperty.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 41 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);

	module.exports = (function () {

		var o = function (graph) {
			BaseProperty.apply(this, arguments);

			this.attributes(["object"])
				.styleClass("objectproperty")
				.type("owl:ObjectProperty");
		};
		o.prototype = Object.create(BaseProperty.prototype);
		o.prototype.constructor = o;

		return o;
	}());




/***/ },
/* 42 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);

	module.exports = (function () {

		var o = function (graph) {
			BaseProperty.apply(this, arguments);

			this.attributes(["symmetric"])
				.styleClass("symmetricproperty")
				.type("owl:SymmetricProperty");
		};
		o.prototype = Object.create(BaseProperty.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 43 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);

	module.exports = (function () {

		var o = function (graph) {
			BaseProperty.apply(this, arguments);

			this.attributes(["transitive"])
				.styleClass("transitiveproperty")
				.type("owl:TransitiveProperty");
		};
		o.prototype = Object.create(BaseProperty.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 44 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);

	module.exports = (function () {

		var o = function (graph) {
			BaseProperty.apply(this, arguments);

			this.attributes(["rdf"])
				.styleClass("rdfproperty")
				.type("rdf:Property");
		};
		o.prototype = Object.create(BaseProperty.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 45 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);

	module.exports = (function () {

		var o = function (graph) {
			BaseProperty.apply(this, arguments);

			var that = this,
				superDrawFunction = that.draw,
				label = "Subclass of";

			this.draw = function (labelGroup) {
				that.labelVisible(!graph.options().compactNotation());
				return superDrawFunction(labelGroup);
			};

			// Disallow overwriting the label
			this.label = function (p) {
				if (!arguments.length) return label;
				return this;
			};

			this.linkType("dotted")
				.markerType("dotted")
				.styleClass("subclass")
				.type("rdfs:subClassOf");
		};
		o.prototype = Object.create(BaseProperty.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 46 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);

	module.exports = (function () {

		var o = function (graph) {
			BaseProperty.apply(this, arguments);

			this.markerType("special")
				.labelVisible(false)
				.linkType("special")
				.styleClass("setoperatorproperty")
				.type("setOperatorProperty");
		};
		o.prototype = Object.create(BaseProperty.prototype);
		o.prototype.constructor = o;

		return o;
	}());


/***/ },
/* 47 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {var math = __webpack_require__(48)();
	var linkCreator = __webpack_require__(49)();
	var elementTools = __webpack_require__(54)();


	module.exports = function(graphContainerSelector) {
	  var graph = {},
	    CARDINALITY_HDISTANCE = 20,
	    CARDINALITY_VDISTANCE = 10,
	    curveFunction = d3.svg.line()
	    .x(function(d) {
	      return d.x;
	    })
	    .y(function(d) {
	      return d.y;
	    })
	    .interpolate("cardinal"),
	    options = __webpack_require__(55)(),
	    parser = __webpack_require__(56)(graph),
	    language = "default",
	    // Container for visual elements
	    graphContainer,
	    nodeContainer,
	    labelContainer,
	    cardinalityContainer,
	    linkContainer,
	    // Visual elements
	    nodeElements,
	    labelGroupElements,
	    linkGroups,
	    linkPathElements,
	    cardinalityElements,
	    // Internal data
	    classNodes,
	    labelNodes,
	    links,
	    properties,
	    unfilteredNodes,
	    unfilteredProperties,
	    // Graph behaviour
	    force,
	    dragBehaviour,
	    zoom;

	  /**
	   * Recalculates the positions of nodes, links, ... and updates them.
	   */
	  function recalculatePositions() {
	    // Set node positions
	    nodeElements.attr("transform", function(node) {
	      return "translate(" + node.x + "," + node.y + ")";
	    });

	    // Set label group positions
	    labelGroupElements.attr("transform", function(label) {
	      var position;

	      // force centered positions on single-layered links
	      var link = label.link();
	      if (link.layers().length === 1 && !link.loops()) {
	        var linkDomainIntersection = math.calculateIntersection(link.range(), link.domain(), 0);
	        var linkRangeIntersection = math.calculateIntersection(link.domain(), link.range(), 0);
	        position = math.calculateCenter(linkDomainIntersection, linkRangeIntersection);
	        label.x = position.x;
	        label.y = position.y;
	      } else {
	        position = label;
	      }

	      return "translate(" + position.x + "," + position.y + ")";
	    });

	    // Set link paths and calculate additional informations
	    linkPathElements.attr("d", function(l) {
	      if (l.isLoop()) {
	        return math.calculateLoopPath(l);
	      }

	      var curvePoint = l.label();
	      var pathStart = math.calculateIntersection(curvePoint, l.domain(), 1);
	      var pathEnd = math.calculateIntersection(curvePoint, l.range(), 1);

	      return curveFunction([pathStart, curvePoint, pathEnd]);
	    });

	    // Set cardinality positions
	    cardinalityElements.attr("transform", function(property) {
	      var label = property.link().label(),
	        pos = math.calculateIntersection(label, property.range(), CARDINALITY_HDISTANCE),
	        normalV = math.calculateNormalVector(label, property.domain(), CARDINALITY_VDISTANCE);

	      return "translate(" + (pos.x + normalV.x) + "," + (pos.y + normalV.y) + ")";
	    });
	  }

	  /**
	   * Adjusts the containers current scale and position.
	   */
	  function zoomed() {
	    graphContainer.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
	  }

	  /**
	   * Initializes the graph.
	   */
	  function initializeGraph() {
	    options.graphContainerSelector(graphContainerSelector);

	    force = d3.layout.force()
	      .on("tick", recalculatePositions);

	    dragBehaviour = d3.behavior.drag()
	      .origin(function(d) {
	        return d;
	      })
	      .on("dragstart", function(d) {
	        d3.event.sourceEvent.stopPropagation(); // Prevent panning
	        d.locked(true);
	      })
	      .on("drag", function(d) {
	        d.px = d3.event.x;
	        d.py = d3.event.y;
	        force.resume();
	      })
	      .on("dragend", function(d) {
	        d.locked(false);
	      });

	    // Apply the zooming factor.
	    zoom = d3.behavior.zoom()
	      .duration(150)
	      .scaleExtent([options.minMagnification(), options.maxMagnification()])
	      .on("zoom", zoomed);

	  }

	  initializeGraph();

	  /**
	   * Returns the graph options of this graph (readonly).
	   * @returns {webvowl.options} a graph options object
	   */
	  graph.graphOptions = function() {
	    return options;
	  };

	  /**
	   * Loads all settings, removes the old graph (if it exists) and draws a new one.
	   */
	  graph.start = function() {
	    force.stop();
	    loadGraphData();
	    redrawGraph();
	    setZoomLevel(0.5);
	    graph.update();
	  };

	  /**
	   * Updates only the style of the graph.
	   */
	  graph.updateStyle = function() {
	    refreshGraphStyle();
	    force.start();
	  };

	  graph.reload = function() {
	    loadGraphData();
	    this.update();
	  };

	  /**
	   * Updates the graphs displayed data and style.
	   */
	  graph.update = function() {
	    refreshGraphData();
	    refreshGraphStyle();
	    force.start();
	    redrawContent();
	  };

	  /**
	   * Stops the influence of the force directed layout on all nodes. They are still manually movable.
	   */
	  graph.freeze = function() {
	    force.nodes().forEach(function(n) {
	      n.frozen(true);
	    });
	  };

	  /**
	   * Allows the influence of the force directed layout on all nodes.
	   */
	  graph.unfreeze = function() {
	    force.nodes().forEach(function(n) {
	      n.frozen(false);
	    });
	    force.resume();
	  };

	  /**
	   * Resets visual settings like zoom or panning.
	   */
	  graph.reset = function() {
	    zoom.translate([0, 0])
	      .scale(1);
	  };

	  /**
	   * Calculate the link distance of a single link part.
	   * The visible link distance does not contain e.g. radii of round nodes.
	   * @param linkPart the link
	   * @returns {*}
	   */
	  function calculateLinkPartDistance(linkPart) {
	    var link = linkPart.link();

	    if (link.isLoop()) {
	      return options.loopDistance();
	    }

	    var completeLinkDistance = getVisibleLinkDistance(link);
	    completeLinkDistance += link.domain().actualRadius();
	    completeLinkDistance += link.range().actualRadius();
	    // divide by 2 to receive the length of a single link part
	    return completeLinkDistance / 2;
	  }

	  function getVisibleLinkDistance(link) {
	    if (elementTools.isDatatype(link.domain()) || elementTools.isDatatype(link.range())) {
	      return options.datatypeDistance();
	    } else {
	      return options.classDistance();
	    }
	  }

	  /**
	   * Empties the last graph container and draws a new one with respect to the
	   * value the graph container selector has.
	   */
	  function redrawGraph() {
	    remove();

	    graphContainer = d3.selectAll(options.graphContainerSelector())
	      .append("svg")
	      .classed("vowlGraph", true)
	      .attr("width", options.width())
	      .attr("height", options.height())
	      .call(zoom)
	      .on("dblclick.zoom", null) //Disable dblclick zoom
	      .append("g");
	  }

	  /**
	   * Redraws all elements like nodes, links, ...
	   */
	  function redrawContent() {
	    var markerContainer;

	    if (!graphContainer) {
	      return;
	    }

	    // Empty the graph container
	    graphContainer.selectAll("*").remove();

	    // Last container -> elements of this container overlap others
	    linkContainer = graphContainer.append("g").classed("linkContainer", true);
	    cardinalityContainer = graphContainer.append("g").classed("cardinalityContainer", true);
	    labelContainer = graphContainer.append("g").classed("labelContainer", true);
	    nodeContainer = graphContainer.append("g").classed("nodeContainer", true);

	    // Add an extra container for all markers
	    markerContainer = linkContainer.append("defs");

	    // Draw nodes
	    nodeElements = nodeContainer.selectAll(".node")
	      .data(classNodes).enter()
	      .append("g")
	      .classed("node", true)
	      .attr("id", function(d) {
	        return d.id();
	      })
	      .call(dragBehaviour);

	    nodeElements.each(function(node) {
	      node.draw(d3.select(this));
	    });

	    // Draw label groups (property + inverse)
	    labelGroupElements = labelContainer.selectAll(".labelGroup")
	      .data(labelNodes).enter()
	      .append("g")
	      .classed("labelGroup", true)
	      .call(dragBehaviour);

	    labelGroupElements.each(function(label) {
	      var success = label.draw(d3.select(this));
	      // Remove empty groups without a label.
	      if (!success) {
	        d3.select(this).remove();
	      }
	    });

	    // Place subclass label groups on the bottom of all labels
	    labelGroupElements.each(function(label) {
	      // the label might be hidden e.g. in compact notation
	      if (!this.parentNode) {
	        return;
	      }

	      if (elementTools.isRdfsSubClassOf(label.property())) {
	        var parentNode = this.parentNode;
	        parentNode.insertBefore(this, parentNode.firstChild);
	      }
	    });

	    // Draw cardinalities
	    cardinalityElements = cardinalityContainer.selectAll(".cardinality")
	      .data(properties).enter()
	      .append("g")
	      .classed("cardinality", true);

	    cardinalityElements.each(function(property) {
	      var success = property.drawCardinality(d3.select(this));

	      // Remove empty groups without a label.
	      if (!success) {
	        d3.select(this).remove();
	      }
	    });

	    // Draw links
	    linkGroups = linkContainer.selectAll(".link")
	      .data(links).enter()
	      .append("g")
	      .classed("link", true);

	    linkGroups.each(function(link) {
	      link.draw(d3.select(this), markerContainer);
	    });

	    // Select the path for direct access to receive a better performance
	    linkPathElements = linkGroups.selectAll("path");

	    addClickEvents();
	  }

	  /**
	   * Applies click listeneres to nodes and properties.
	   */
	  function addClickEvents() {
	    function executeModules(selectedElement) {
	      options.selectionModules().forEach(function(module) {
	        module.handle(selectedElement);
	      });
	    }

	    nodeElements.on("click", function(clickedNode) {
	      executeModules(clickedNode);
	    });

	    nodeElements.on("dblclick", function(clickedNode) {
	      loadGraphDataByClass(clickedNode.id());
	      if (window.parent.selectNodeInTree !== undefined) {
	        window.parent.selectNodeInTree(clickedNode.id());
	      }
	    });

	    labelGroupElements.selectAll(".label").on("click", function(clickedProperty) {
	      executeModules(clickedProperty);
	    });
	  }

	  var classHierarchy;

	  function loadGraphData() {
	    var theSubset;
	    if (options.data() !== undefined) {
	      var a = performance.now();
	      classHierarchy = buildClassHierarchy(options.data()); //~ 26ms pre-cache
	      var b = performance.now();
	      // alert('It took buildClassHierarchy ' + (b - a) + ' ms.');
	      //  printTreeAll(classHierarchy);
	      //Update the Treeview
	      // var scope = angular.element(document.getElementById("TreeViewDiv")).scope();
	      // scope.$apply(function() {
	      //   var treeViewModel = [];
	      //   var thing = {
	      //     id: "Thing",
	      //     name: "Thing",
	      //     children: []
	      //   };
	      //   treeViewModel.push(thing);
	      //   for (var i = 0; i < classHierarchy.topLevel.length; i++) {
	      //     if (classHierarchy.topLevel[i].name !== "Thing") { //Hide Thing from Treeview. Why is there more than one Thing? -> OWL2VOWL makes multiple things to produce a nicer looking force graph.
	      //       thing.children.push(classHierarchy.topLevel[i]);
	      //     }
	      //   }
	      //   scope.refreshTreeData(treeViewModel);
	      // });

				// theSubset = options.data();
	      theSubset = getontologyJsonSubset("Thing", options.data(), classHierarchy);

	    }
	    parser.parse(theSubset);
	    unfilteredNodes = parser.nodes();
	    unfilteredProperties = parser.properties();
	  }

	  window.loadFullDataSet = function(data) {
	    options.data(data);
	    loadGraphData();
	    // graph.update();
	  };
	  var loadGraphDataByClass = function(classId) {
	    var theSubset = getontologyJsonSubset(classId, options.data(), classHierarchy);
	    parser.parse(theSubset);
	    unfilteredNodes = parser.nodes();
	    unfilteredProperties = parser.properties();
	    setZoomLevel(zoom.scale());
	    graph.update();
	    pinClass(classId);
	  };

	  function setZoomLevel(scale) {
	    var zoomWidth = (options.width() - scale * options.width()) / 2;
	    var zoomHeight = (options.height() - scale * options.height()) / 2;
	    zoom.translate([zoomWidth, zoomHeight]).scale(scale);
	  }

	  window.loadGraphDataByClass = loadGraphDataByClass;
		window.setZoomLevel = setZoomLevel;

	  function pinClass(classId) {
	    var nodeElements = nodeContainer.selectAll(".node");
	    nodeElements.each(function(node) {
	      if (node.id() === classId) {
	        if (!node.focused()) {
	          node.toggleFocus();
	        }
	        node.drawPin();
	        //Center Node
	        node.px = nodeContainer[0].parentNode.clientWidth / 2;
	        node.py = nodeContainer[0].parentNode.clientHeight / 2;
	      }
	    });
	  }

	  function getontologyJsonSubset(startingClassId, ontologyJson, myClassHierarchy) {
	    /* jshint shadow:true */
	    //var ontologyJson =JSON.parse(JSON.stringify(ontologyJson));
	    var theSubset = {
	      "_comment": "Created with OWL2VOWL (version 0.2.2-SNAPSHOT), http://vowl.visualdataweb.org",
	      "namespace": [],
	      "header": {
	        "languages": ["IRI-based", "undefined"],
	        "iri": "file://securboration/main/resources/owl/transcom/metatagger/1.0"
	      },
	      "metrics": {
	        "classCount": 4742,
	        "datatypeCount": 90,
	        "objectPropertyCount": 355,
	        "datatypePropertyCount": 90,
	        "propertyCount": 445,
	        "nodeCount": 4832,
	        "axiomCount": 41345,
	        "individualCount": 3322
	      },
	      class: [],
	      classAttribute: [],
	      datatype: [],
	      datatypeAttribute: [],
	      property: [],
	      propertyAttribute: []
	    };

	    // copyArray(ontologyJson.class, theSubset.class);
	    // copyArray(ontologyJson.classAttribute, theSubset.classAttribute);
	    //  copyArray(ontologyJson.datatype, theSubset.datatype);
	    //  copyArray(ontologyJson.datatypeAttribute, theSubset.datatypeAttribute);
	    // copyArray(ontologyJson.property, theSubset.property);
	    // copyArray(ontologyJson.propertyAttribute, theSubset.propertyAttribute);

	    var filtedClassIds = [];
	    if (startingClassId === "Thing") {
	      for (var i = 0; i < myClassHierarchy.topLevel.length; i++) {
	        pushItemIfNotExist(myClassHierarchy.topLevel[i].id, filtedClassIds);
	        for (var j = 0; j < myClassHierarchy.topLevel[i].children.length; j++) {
	          pushItemIfNotExist(myClassHierarchy.topLevel[i].children[j].id, filtedClassIds);
	        }
	      }
	    } else {
	      var startingClass = myClassHierarchy.cache.classAttribute[startingClassId];
	      pushItemIfNotExist(startingClass.id, filtedClassIds);
	      //Walk up one level if possible
	      if (startingClass.superClassesIntact !== undefined) {
	        for (var i = 0; i < startingClass.superClassesIntact.length; i++) {
	          var startClassParent = myClassHierarchy.cache.classAttribute[startingClass.superClassesIntact[i]];
	          pushItemIfNotExist(startClassParent.id, filtedClassIds);
	          //For Symmetry include the parent's subclasses as well
	          if (startClassParent.subClassesIntact !== undefined) {
	            for (var p = 0; p < startClassParent.subClassesIntact.length; p++) {
	              pushItemIfNotExist(startClassParent.subClassesIntact[p], filtedClassIds);
	            }
	          }
	        }
	      }
	      for (var j = 0; j < startingClass.children.length; j++) {
	        var lvl1 = startingClass.children[j];
	        pushItemIfNotExist(lvl1.id, filtedClassIds);
	        for (var h = 0; h < lvl1.children.length; h++) {
	          var lvl2 = lvl1.children[h];
	          pushItemIfNotExist(lvl2.id, filtedClassIds);
	          //  for (var k = 0; k < lvl2.children.length; k++) {
	          // 	 var lvl3 = lvl2.children[k];
	          // 	 pushItemIfNotExist(lvl3.id, filtedClassIds);
	          // }
	        }
	      }
	    }


	    //classAttribute
	    for (var i = 0; i < filtedClassIds.length; i++) {
	      var ca = myClassHierarchy.cache.classAttribute[filtedClassIds[i]];
	      //Remove
	      if (ca.subClasses !== undefined) {
	        if (ca.subClassesIntact === undefined) {
	          ca.subClassesIntact = [];
	          copyArray(ca.subClasses, ca.subClassesIntact);
	        }
	        ca.subClasses = ca.subClassesIntact.filter(function(x) {
	          return filtedClassIds.indexOf(x) !== -1;
	        });
	      }
	      if (ca.superClasses !== undefined) {
	        if (ca.superClassesIntact === undefined) {
	          ca.superClassesIntact = [];
	          copyArray(ca.superClasses, ca.superClassesIntact);
	        }
	        ca.superClasses = ca.superClasses.filter(function(x) {
	          return filtedClassIds.indexOf(x) !== -1;
	        });
	      }
	      if (ca.equivalent !== undefined) {
	        if (ca.equivalentIntact === undefined) {
	          ca.equivalentIntact = [];
	          copyArray(ca.equivalent, ca.equivalentIntact);
	        }
	        ca.equivalent = ca.equivalent.filter(function(x) {
	          return filtedClassIds.indexOf(x) !== -1;
	        });
	      }
	      if (ca.intersection !== undefined) {
	        if (ca.intersectionIntact === undefined) {
	          ca.intersectionIntact = [];
	          copyArray(ca.intersection, ca.intersectionIntact);
	        }
	        ca.intersection = ca.intersection.filter(function(x) {
	          return filtedClassIds.indexOf(x) !== -1;
	        });
	      }
	      if (ca.union !== undefined) { //TODO: I'm not sure if unions should be treated in the same manner as sub/super classes
	        if (ca.unionIntact === undefined) {
	          ca.unionIntact = [];
	          copyArray(ca.union, ca.unionIntact);
	        }
	        ca.union = ca.union.filter(function(x) {
	          return filtedClassIds.indexOf(x) !== -1;
	        });
	      }
	      theSubset.classAttribute.push(ca);
	    }
	    //propertyAttribute
	    var filtedPropertyIds = [];
	    for (var i = 0; i < ontologyJson.propertyAttribute.length; i++) {
	      var pa = ontologyJson.propertyAttribute[i];
	      var propertyHit = false;
	      if (filtedClassIds.indexOf(pa.domain) !== -1 && filtedClassIds.indexOf(pa.range) !== -1) { //Both classes are in filtedClassIds
	        propertyHit = true;
	      } else if (filtedClassIds.indexOf(pa.domain) !== -1 && myClassHierarchy.thingIds.indexOf(pa.range) !== -1) { //Find classes that link to thing
	        propertyHit = true;
	        var thingId = pa.range;
	        if (filtedClassIds.indexOf(thingId) === -1) {
	          filtedClassIds.push(thingId);
	          theSubset.classAttribute.push(myClassHierarchy.cache.classAttribute[thingId]);
	        }
	      } else if (filtedClassIds.indexOf(pa.range) !== -1 && myClassHierarchy.thingIds.indexOf(pa.domain) !== -1) { //Find classes that link to thing
	        propertyHit = true;
	        var thingId = pa.domain;
	        if (filtedClassIds.indexOf(thingId) === -1) {
	          filtedClassIds.push(thingId);
	          theSubset.classAttribute.push(myClassHierarchy.cache.classAttribute[thingId]);
	        }
	      } else if ((filtedClassIds.indexOf(pa.domain) !== -1 || (startingClassId === "Thing" && myClassHierarchy.thingIds.indexOf(pa.domain) !== -1)) &&  (pa.range.startsWith("literal") || pa.range.startsWith("datatype"))) { //Find classes that link to a literal or datatype.
	        propertyHit = true;
					var thingId = pa.domain;
					if (filtedClassIds.indexOf(thingId) === -1) {
						filtedClassIds.push(thingId);
						theSubset.classAttribute.push(myClassHierarchy.cache.classAttribute[thingId]);
					}

	        var literalId = pa.range;
					var datatype = ontologyJson.datatype.filter(function(a) {
			      return a.id === literalId;
			    })[0];
					theSubset.datatype.push(datatype);

					var datatypeAttribute = ontologyJson.datatypeAttribute.filter(function(a) {
			      return a.id === literalId;
			    })[0];
					theSubset.datatypeAttribute.push(datatypeAttribute);
	      }
	      if (propertyHit === true) {
	        pushItemIfNotExist(pa.id, filtedPropertyIds);
	        //  delete pa.subproperty;
	        //  delete pa.superproperty;
	        //  delete pa.inverse;
	      }
	    }
	    //class
	    for (var i = 0; i < filtedClassIds.length; i++) {
	      theSubset.class.push(myClassHierarchy.cache.class[filtedClassIds[i]]);
	    }
	    //propertyAttribute/property
	    for (var i = 0; i < filtedPropertyIds.length; i++) {
	      theSubset.propertyAttribute.push(myClassHierarchy.cache.propertyAttribute[filtedPropertyIds[i]]);
	      theSubset.property.push(myClassHierarchy.cache.property[filtedPropertyIds[i]]);
	    }
	    return theSubset;
	  }

	  function pushItemIfNotExist(item, dest) {
	    if (dest.indexOf(item) === -1) {
	      dest.push(item);
	    }
	  }

	  function pushIfNotExist(source, dest) {
	    for (var i = 0; i < source.length; i++) {
	      if (dest.indexOf(source[i]) === -1) {
	        dest.push(source[i]);
	      }
	    }
	  }
	  // function copyFromCache(sourceHashMap, destArray){
	  // 	for (var i = 0; i <  source.length; i++) {
	  // 		destHashMap[source[i].id] =  source[i];
	  // 	}
	  // }

	  function copyArray(source, dest) {
	    for (var i = 0; i < source.length; i++) {
	      dest.push(source[i]);
	    }
	  }

	  function buildCache(source, destHashMap) {
	    for (var i = 0; i < source.length; i++) {
	      destHashMap[source[i].id] = source[i];
	    }
	  }

	  function buildClassHierarchy(ontologyJson) {
	    // console.clear();
	    var myClassHierarchy = {
	      cache: {
	        class: {},
	        classAttribute: {},
	        datatype: {},
	        datatypeAttribute: {},
	        property: {},
	        propertyAttribute: {}
	      },
	      topLevel: [],
	      things: [],
	      thingIds: []
	    };
	    buildCache(ontologyJson.class, myClassHierarchy.cache.class);
	    // buildCache(ontologyJson.classAttribute, myClassHierarchy.cache.classAttribute);
	    // buildCache(ontologyJson.datatype, myClassHierarchy.cache.datatype);
	    // buildCache(ontologyJson.datatypeAttribute, myClassHierarchy.cache.datatypeAttribute);
	    buildCache(ontologyJson.property, myClassHierarchy.cache.property);
	    buildCache(ontologyJson.propertyAttribute, myClassHierarchy.cache.propertyAttribute);

	    var excludeClassTypes = ["rdfs:Datatype", "rdfs:Literal"];
	      //Find top level nodes/classAttributes
	      for (var i = 0; i < ontologyJson.classAttribute.length; i++) {
	        //Add name field. Used for the Tree display
	        var classType = myClassHierarchy.cache.class[ontologyJson.classAttribute[i].id].type;
	        if (excludeClassTypes.indexOf(classType) > -1) {
	          ontologyJson.classAttribute[i].name = "";
	        } else if (ontologyJson.classAttribute[i].label === undefined) {
	          ontologyJson.classAttribute[i].name = "";
	        } else {
	          if (ontologyJson.classAttribute[i].label["undefined"] !== undefined) { //Use label if it exist
	            ontologyJson.classAttribute[i].name = ontologyJson.classAttribute[i].label["undefined"];
	          } else if (ontologyJson.classAttribute[i].label["IRI-based"] !== undefined) {
	            ontologyJson.classAttribute[i].name = ontologyJson.classAttribute[i].label["IRI-based"];
	          } else {
	            console.log("Should not happen: Both Label and IRI-based Id are empty");
	            ontologyJson.classAttribute[i].name = "";
	          }
	        }

	      myClassHierarchy.cache.classAttribute[ontologyJson.classAttribute[i].id] = ontologyJson.classAttribute[i]; //Add classAttribute to cache
	      if (ontologyJson.classAttribute[i].superClasses === undefined) { //These are top level classes
	        if (ontologyJson.classAttribute[i].name === "Thing") {
	          myClassHierarchy.things.push(ontologyJson.classAttribute[i]);
	          myClassHierarchy.thingIds.push(ontologyJson.classAttribute[i].id);
	        } else if (ontologyJson.classAttribute[i].name !== "" && ontologyJson.classAttribute[i].name !== "anonymous") {
	          myClassHierarchy.topLevel.push(ontologyJson.classAttribute[i]);
	        }
	      }
	    }
	    //Sort top level nodes
	    sortClassArray(myClassHierarchy.topLevel);
	    //Recursively add children
	    for (var j = 0; j < myClassHierarchy.topLevel.length; j++) {
	      buildClassHierarchyChildren(myClassHierarchy.topLevel[j], myClassHierarchy.cache.classAttribute, 0);
	    }
	    return myClassHierarchy;
	  }

	  function buildClassHierarchyChildren(theClass, classCacheByClassId, depth) {
	    // if (depth === 1) {
	    // 	return;
	    // }
	    theClass.children = []; //Create children array
	    //Create a copy of subClasses/superClasses/equivalent as the original arrays will be modified for the graph.
	    if (theClass.subClasses !== undefined) {
	      if (theClass.subClassesIntact === undefined) {
	        theClass.subClassesIntact = [];
	        copyArray(theClass.subClasses, theClass.subClassesIntact);
	      }
	    }
	    if (theClass.superClasses !== undefined) {
	      if (theClass.superClassesIntact === undefined) {
	        theClass.superClassesIntact = [];
	        copyArray(theClass.superClasses, theClass.superClassesIntact);
	      }
	    }
	    if (theClass.equivalent !== undefined) {
	      if (theClass.equivalentIntact === undefined) {
	        theClass.equivalentIntact = [];
	        copyArray(theClass.equivalent, theClass.equivalentIntact);
	      }
	    }
	    //Structure class attibutes into a object tree
	    if (theClass.subClasses !== undefined) {
	      for (var sc = 0; sc < theClass.subClasses.length; sc++) {
	        var subClass = classCacheByClassId[theClass.subClasses[sc]];
	        theClass.children.push(subClass);
	        buildClassHierarchyChildren(subClass, classCacheByClassId, depth += 1);
	      }
	      sortClassArray(theClass.children);
	    }
	  }

	  function sortClassArray(classArray) {
	    classArray.sort(function(a, b) {
	      var aKey = a.name.toUpperCase();
	      var bKey = b.name.toUpperCase();
	      if (aKey > bKey) {
	        return 1;
	      } else if (aKey < bKey) {
	        return -1;
	      }
	      return 0;
	    });
	  }

	  var treeDivInner = "";
	  var classcnt = 0;

	  function printTreeAll(classHierarchy) {
	    treeDivInner = "";
	    classcnt = 0;
	    printTree(classHierarchy.topLevel, 1);
	    //  document.getElementById('myTreeDiv').innerHTML = "Class Count: " + classcnt + "<br>" + treeDivInner;
	    document.getElementById('myTreeDiv').innerHTML = treeDivInner;
	  }

	  function printTree(classArray, depth) {
	    for (var i = 0; i < classArray.length; i++) {
	      classcnt += 1;
	      treeDivInner += Array(depth).join("&nbsp;") + classArray[i].name + "<br>";
	      if (classArray[i].children !== undefined) {
	        printTree(classArray[i].children, depth += 1);
	      }
	    }
	  }

	  /**
	   * Applies the data of the graph options object and parses it. The graph is not redrawn.
	   */
	  function refreshGraphData() {
	    var preprocessedNodes = unfilteredNodes,
	      preprocessedProperties = unfilteredProperties;

	    // Filter the data
	    options.filterModules().forEach(function(module) {
	      links = linkCreator.createLinks(preprocessedProperties);
	      storeLinksOnNodes(preprocessedNodes, links);

	      module.filter(preprocessedNodes, preprocessedProperties);
	      preprocessedNodes = module.filteredNodes();
	      preprocessedProperties = module.filteredProperties();
	    });

	    classNodes = preprocessedNodes;
	    properties = preprocessedProperties;
	    links = linkCreator.createLinks(properties);
	    labelNodes = links.map(function(link) {
	      return link.label();
	    });
	    storeLinksOnNodes(classNodes, links);

	    setForceLayoutData(classNodes, labelNodes, links);
	  }

	  function storeLinksOnNodes(nodes, links) {
	    for (var i = 0, nodesLength = nodes.length; i < nodesLength; i++) {
	      var node = nodes[i],
	        connectedLinks = [];

	      // look for properties where this node is the domain or range
	      for (var j = 0, linksLength = links.length; j < linksLength; j++) {
	        var link = links[j];

	        if (link.domain() === node || link.range() === node) {
	          connectedLinks.push(link);
	        }
	      }

	      node.links(connectedLinks);
	    }
	  }

	  function setForceLayoutData(classNodes, labelNodes, links) {
	    var d3Links = [];
	    links.forEach(function(link) {
	      d3Links = d3Links.concat(link.linkParts());
	    });

	    var d3Nodes = [].concat(classNodes).concat(labelNodes);
	    setPositionOfOldLabelsOnNewLabels(force.nodes(), labelNodes);

	    force.nodes(d3Nodes)
	      .links(d3Links);
	  }

	  /**
	   * The label nodes are positioned randomly, because they are created from scratch if the data changes and lose
	   * their position information. With this hack the position of old labels is copied to the new labels.
	   */
	  function setPositionOfOldLabelsOnNewLabels(oldLabelNodes, labelNodes) {
	    labelNodes.forEach(function(labelNode) {
	      for (var i = 0; i < oldLabelNodes.length; i++) {
	        var oldNode = oldLabelNodes[i];
	        if (oldNode.equals(labelNode)) {
	          labelNode.x = oldNode.x;
	          labelNode.y = oldNode.y;
	          break;
	        }
	      }
	    });
	  }


	  /**
	   * Applies all options that don't change the graph data.
	   */
	  function refreshGraphStyle() {
	    zoom = zoom.scaleExtent([options.minMagnification(), options.maxMagnification()]);
	    if (graphContainer) {
	      zoom.event(graphContainer);
	    }

	    force.charge(function(element) {
	        var charge = options.charge();
	        if (elementTools.isLabel(element)) {
	          charge *= 0.8;
	        }
	        return charge;
	      })
	      .size([options.width(), options.height()])
	      .linkDistance(calculateLinkPartDistance)
	      .gravity(options.gravity())
	      .linkStrength(options.linkStrength()); // Flexibility of links
	  }

	  /**
	   * Removes all elements from the graph container.
	   */
	  function remove() {
	    if (graphContainer) {
	      // Select the parent element because the graph container is a group (e.g. for zooming)
	      d3.select(graphContainer.node().parentNode).remove();
	    }
	  }

	  graph.options = function() {
	    return options;
	  };

	  graph.language = function(newLanguage) {
	    if (!arguments.length) return language;

	    // Just update if the language changes
	    if (language !== newLanguage) {
	      language = newLanguage || "default";
	      redrawContent();
	      recalculatePositions();
	    }
	    return graph;
	  };


	  return graph;
	};

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 48 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {/**
	 * Contains a collection of mathematical functions with some additional data
	 * used for WebVOWL.
	 */
	module.exports = (function () {

		var math = {},
			loopFunction = d3.svg.line()
				.x(function (d) {
					return d.x;
				})
				.y(function (d) {
					return d.y;
				})
				.interpolate("cardinal")
				.tension(-1);


		/**
		 * Calculates the normal vector of the path between the two nodes.
		 * @param source the first node
		 * @param target the second node
		 * @param length the length of the calculated normal vector
		 * @returns {{x: number, y: number}}
		 */
		math.calculateNormalVector = function (source, target, length) {
			var dx = target.x - source.x,
				dy = target.y - source.y;

			var nx = -dy,
				ny = dx;

			var vlength = Math.sqrt(nx * nx + ny * ny);

			var ratio = vlength !== 0 ? length / vlength : 0;

			return {"x": nx * ratio, "y": ny * ratio};
		};

		/**
		 * Calculates the path for a link, if it is a loop. Currently only working for circlular nodes.
		 * @param link the link
		 * @returns {*}
		 */
		math.calculateLoopPath = function (link) {
			var node = link.domain(),
				label = link.label();

			var fairShareLoopAngle = 360 / link.loops().length,
				fairShareLoopAngleWithMargin = fairShareLoopAngle * 0.8,
				loopAngle = Math.min(60, fairShareLoopAngleWithMargin);

			var dx = label.x - node.x,
				dy = label.y - node.y,
				labelRadian = Math.atan2(dy, dx),
				labelAngle = calculateAngle(labelRadian);

			var startAngle = labelAngle - loopAngle / 2,
				endAngle = labelAngle + loopAngle / 2;

			var arcFrom = calculateRadian(startAngle),
				arcTo = calculateRadian(endAngle),

				x1 = Math.cos(arcFrom) * node.actualRadius(),
				y1 = Math.sin(arcFrom) * node.actualRadius(),

				x2 = Math.cos(arcTo) * node.actualRadius(),
				y2 = Math.sin(arcTo) * node.actualRadius(),

				fixPoint1 = {"x": node.x + x1, "y": node.y + y1},
				fixPoint2 = {"x": node.x + x2, "y": node.y + y2};

			return loopFunction([fixPoint1, link.label(), fixPoint2]);
		};

		/**
		 * @param angle
		 * @returns {number} the radian of the angle
		 */
		function calculateRadian(angle) {
			angle = angle % 360;
			if (angle < 0) {
				angle = angle + 360;
			}
			return (Math.PI * angle) / 180;
		}

		/**
		 * @param radian
		 * @returns {number} the angle of the radian
		 */
		function calculateAngle(radian) {
			return radian * (180 / Math.PI);
		}

		/**
		 * Calculates the point where the link between the source and target node
		 * intersects the border of the target node.
		 * @param source the source node
		 * @param target the target node
		 * @param additionalDistance additional distance the
		 * @returns {{x: number, y: number}}
		 */
		math.calculateIntersection = function (source, target, additionalDistance) {
			var dx = target.x - source.x,
				dy = target.y - source.y,
				length = Math.sqrt(dx * dx + dy * dy);

			if (length === 0) {
				return {x: source.x, y: source.y};
			}

			var innerDistance = target.distanceToBorder(dx, dy);

			var ratio = (length - (innerDistance + additionalDistance)) / length,
				x = dx * ratio + source.x,
				y = dy * ratio + source.y;

			return {x: x, y: y};
		};

		/**
		 * Calculates the position between the two points.
		 * @param firstPoint
		 * @param secondPoint
		 * @returns {{x: number, y: number}}
		 */
		math.calculateCenter = function (firstPoint, secondPoint) {
			return {
				x: (firstPoint.x + secondPoint.x) / 2,
				y: (firstPoint.y + secondPoint.y) / 2
			};
		};


		return function () {
			/* Use a function here to keep a consistent style like webvowl.path.to.module()
			 * despite having just a single math object. */
			return math;
		};
	})();

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 49 */
/***/ function(module, exports, __webpack_require__) {

	/**
	 * Stores the passed properties in links.
	 * @returns {Function}
	 */
	module.exports = (function () {
		var linkCreator = {};

		/**
		 * Creates links from the passed properties.
		 * @param properties
		 */
		linkCreator.createLinks = function (properties) {
			var links = groupPropertiesToLinks(properties);

			for (var i = 0, l = links.length; i < l; i++) {
				var link = links[i];

				countAndSetLayers(link, links);
				countAndSetLoops(link, links);
			}

			return links;
		};

		/**
		 * Creates links of properties and - if existing - their inverses.
		 * @param properties the properties
		 * @returns {Array}
		 */
		function groupPropertiesToLinks(properties) {
			var links = [],
				property,
				addedProperties = __webpack_require__(50)();

			for (var i = 0, l = properties.length; i < l; i++) {
				property = properties[i];

				if (!addedProperties.has(property)) {
					var link = __webpack_require__(51)(property.domain(), property.range(), property);

					property.link(link);
					if (property.inverse()) {
						property.inverse().link(link);
					}

					links.push(link);

					addedProperties.add(property);
					if (property.inverse()) {
						addedProperties.add(property.inverse());
					}
				}
			}

			return links;
		}

		function countAndSetLayers(link, allLinks) {
			var layer,
				layers,
				i, l;

			if (typeof link.layers() === "undefined") {
				layers = [];

				// Search for other links that are another layer
				for (i = 0, l = allLinks.length; i < l; i++) {
					var otherLink = allLinks[i];
					if (link.domain() === otherLink.domain() && link.range() === otherLink.range() ||
						link.domain() === otherLink.range() && link.range() === otherLink.domain()) {
						layers.push(otherLink);
					}
				}

				// Set the results on each of the layers
				for (i = 0, l = layers.length; i < l; ++i) {
					layer = layers[i];

					layer.layerIndex(i);
					layer.layers(layers);
				}
			}
		}

		function countAndSetLoops(link, allLinks) {
			var loop,
				loops,
				i, l;

			if (typeof link.loops() === "undefined") {
				loops = [];

				// Search for other links that are also loops of the same node
				for (i = 0, l = allLinks.length; i < l; i++) {
					var otherLink = allLinks[i];
					if (link.domain() === otherLink.domain() && link.domain() === otherLink.range()) {
						loops.push(otherLink);
					}
				}

				// Set the results on each of the loops
				for (i = 0, l = loops.length; i < l; ++i) {
					loop = loops[i];

					loop.loopIndex(i);
					loop.loops(loops);
				}
			}
		}


		return function () {
			// Return a function to keep module interfaces consistent
			return linkCreator;
		};
	})();


/***/ },
/* 50 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {/**
	 * A simple incomplete encapsulation of the d3 set, which is able to store webvowl
	 * elements by using their id.
	 */
	module.exports = function (array) {

		var set = {},
			d3Set = d3.set(array);

		set.has = function (webvowlElement) {
			return d3Set.has(webvowlElement.id());
		};

		set.add = function (webvowlElement) {
			return d3Set.add(webvowlElement.id());
		};

		set.remove = function (webvowlElement) {
			return d3Set.remove(webvowlElement.id());
		};

		set.empty = function () {
			return d3Set.empty();
		};

		set.size = function () {
			return d3Set.size();
		};

		return set;
	};

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 51 */
/***/ function(module, exports, __webpack_require__) {

	var Label = __webpack_require__(52);

	/**
	 * A link connects at least two VOWL nodes.
	 * The properties connecting the VOWL nodes are stored separately into the label.
	 * @param _domain
	 * @param _range
	 * @param _property
	 * @returns {{}}
	 */
	module.exports = function (_domain, _range, _property) {
		var link = {},
			domain = _domain,
			layers,
			layerIndex,
			loops,
			loopIndex,
			label = new Label(_property, link),
			range = _range;

		var backPart = __webpack_require__(53)(domain, label, link),
			frontPart = __webpack_require__(53)(label, range, link);


		link.layers = function (p) {
			if (!arguments.length) return layers;
			layers = p;
			return link;
		};

		link.layerIndex = function (p) {
			if (!arguments.length) return layerIndex;
			layerIndex = p;
			return link;
		};

		link.loops = function (p) {
			if (!arguments.length) return loops;
			loops = p;
			return link;
		};

		link.loopIndex = function (p) {
			if (!arguments.length) return loopIndex;
			loopIndex = p;
			return link;
		};


		link.domain = function () {
			return domain;
		};

		link.inverse = function () {
			return label.inverse();
		};

		link.isLoop = function() {
			return domain === range;
		};

		link.label = function () {
			return label;
		};

		link.linkParts = function () {
			return [frontPart, backPart];
		};

		link.property = function () {
			return label.property();
		};

		link.range = function () {
			return range;
		};


		link.draw = function (linkGroup, markerContainer) {
			var property = label.property();
			var inverse = label.inverse();

			property.linkGroup(linkGroup);
			if (inverse) {
				inverse.linkGroup(linkGroup);
			}

			// Marker for this property
			property.markerElement(markerContainer.append("marker")
				.datum(property)
				.attr("id", property.markerId())
				.attr("viewBox", "0 -8 14 16")
				.attr("refX", 12)
				.attr("refY", 0)
				.attr("markerWidth", 12)  // ArrowSize
				.attr("markerHeight", 12)
				.attr("markerUnits", "userSpaceOnUse")
				.attr("orient", "auto")  // Orientation of Arrow
				.attr("class", property.markerType() + "Marker"));
			property.markerElement()
				.append("path")
				.attr("d", "M0,-8L12,0L0,8Z");

			// Marker for the inverse property
			if (inverse) {
				inverse.markerElement(markerContainer.append("marker")
					.datum(inverse)
					.attr("id", inverse.markerId())
					.attr("viewBox", "0 -8 14 16")
					.attr("refX", 0)
					.attr("refY", 0)
					.attr("markerWidth", 12)  // ArrowSize
					.attr("markerHeight", 12)
					.attr("markerUnits", "userSpaceOnUse")
					.attr("orient", "auto")  // Orientation of Arrow
					.attr("class", inverse.markerType() + "Marker"));
				inverse.markerElement().append("path")
					.attr("d", "M12,-8L0,0L12,8Z");
			}

			// Draw the link
			linkGroup.append("path")
				.classed("link-path", true)
				.classed(domain.cssClassOfNode(), true)
				.classed(range.cssClassOfNode(), true)
				.classed(property.linkType(), true)
				.attr("marker-end", function (l) {
					if (!l.label().property().isSpecialLink()) {
						return "url(#" + l.label().property().markerId() + ")";
					}
					return "";
				})
				.attr("marker-start", function (l) {
					var inverse = l.label().inverse();
					if (inverse && !inverse.isSpecialLink()) {
						return "url(#" + inverse.markerId() + ")";
					}
					return "";
				});
		};


		return link;
	};


/***/ },
/* 52 */
/***/ function(module, exports, __webpack_require__) {

	var forceLayoutNodeFunctions = __webpack_require__(13)();


	module.exports = Label;

	/**
	 * A label represents the element(s) which further describe a link.
	 * It encapsulates the property and its inverse property.
	 * @param property the property; the inverse is inferred
	 * @param link the link this label belongs to
	 */
	function Label(property, link) {
		forceLayoutNodeFunctions.addTo(this);

		this.link = function () {
			return link;
		};

		this.property = function () {
			return property;
		};
	}

	Label.prototype.actualRadius = function () {
		return this.property().actualRadius();
	};

	Label.prototype.draw = function (container) {
		return this.property().draw(container);
	};

	Label.prototype.inverse = function () {
		return this.property().inverse();
	};

	Label.prototype.equals = function (other) {
		if (!other) {
			return false;
		}

		var instance = other instanceof Label;
		var equalProperty = this.property().equals(other.property());

		var equalInverse = false;
		if (this.inverse()) {
			equalInverse = this.inverse().equals(other.inverse());
		} else if (!other.inverse()) {
			equalInverse = true;
		}

		return instance && equalProperty && equalInverse;
	};


/***/ },
/* 53 */
/***/ function(module, exports) {

	/**
	 * A linkPart connects two force layout nodes.
	 * It reprents a link which can be used in d3's force layout.
	 * @param _domain
	 * @param _range
	 * @param _link
	 */
	module.exports = function (_domain, _range, _link) {
		var linkPart = {},
			domain = _domain,
			link = _link,
			range = _range;

		// Define d3 properties
		Object.defineProperties(linkPart, {
			"source": {value: domain, writable: true},
			"target": {value: range, writable: true}
		});


		linkPart.domain = function () {
			return domain;
		};

		linkPart.link = function () {
			return link;
		};

		linkPart.range = function () {
			return range;
		};


		return linkPart;
	};


/***/ },
/* 54 */
/***/ function(module, exports, __webpack_require__) {

	var BaseProperty = __webpack_require__(35);
	var BaseNode = __webpack_require__(9);
	var DatatypeNode = __webpack_require__(28);
	var ObjectProperty = __webpack_require__(41);
	var DatatypeProperty = __webpack_require__(34);
	var RdfsSubClassOf = __webpack_require__(45);
	var Label = __webpack_require__(52);

	module.exports = (function () {
		var tools = {};

		tools.isLabel = function (element) {
			return element instanceof Label;
		};

		tools.isNode = function (element) {
			return element instanceof BaseNode;
		};

		tools.isDatatype = function (node) {
			return node instanceof DatatypeNode;
		};

		tools.isProperty = function(element) {
			return element instanceof BaseProperty;
		};

		tools.isObjectProperty = function (element) {
			return element instanceof ObjectProperty;
		};

		tools.isDatatypeProperty = function (element) {
			return element instanceof DatatypeProperty;
		};

		tools.isRdfsSubClassOf = function (property) {
			return property instanceof RdfsSubClassOf;
		};

		return function () {
			return tools;
		};
	})();


/***/ },
/* 55 */
/***/ function(module, exports) {

	module.exports = function () {
		/**
		 * @constructor
		 */
		var options = {},
			data,
			graphContainerSelector,
			classDistance = 200,
			datatypeDistance = 120,
			loopDistance = 100,
			charge = -500,
			gravity = 0.025,
			linkStrength = 1,
			height = 600,
			width = 800,
			selectionModules = [],
			filterModules = [],
			minMagnification = 0.1,
			maxMagnification = 4,
			compactNotation = false,
			scaleNodesByIndividuals = false;


		options.charge = function (p) {
			if (!arguments.length) return charge;
			charge = +p;
			return options;
		};

		options.classDistance = function (p) {
			if (!arguments.length) return classDistance;
			classDistance = +p;
			return options;
		};

		options.compactNotation = function (p) {
			if (!arguments.length) return compactNotation;
			compactNotation = p;
			return options;
		};

		options.data = function (p) {
			if (!arguments.length) return data;
			data = p;
			return options;
		};

		options.datatypeDistance = function (p) {
			if (!arguments.length) return datatypeDistance;
			datatypeDistance = +p;
			return options;
		};

		options.filterModules = function (p) {
			if (!arguments.length) return filterModules;
			filterModules = p;
			return options;
		};

		options.graphContainerSelector = function (p) {
			if (!arguments.length) return graphContainerSelector;
			graphContainerSelector = p;
			return options;
		};

		options.gravity = function (p) {
			if (!arguments.length) return gravity;
			gravity = +p;
			return options;
		};

		options.height = function (p) {
			if (!arguments.length) return height;
			height = +p;
			return options;
		};

		options.linkStrength = function (p) {
			if (!arguments.length) return linkStrength;
			linkStrength = +p;
			return options;
		};

		options.loopDistance = function (p) {
			if (!arguments.length) return loopDistance;
			loopDistance = p;
			return options;
		};

		options.minMagnification = function (p) {
			if (!arguments.length) return minMagnification;
			minMagnification = +p;
			return options;
		};

		options.maxMagnification = function (p) {
			if (!arguments.length) return maxMagnification;
			maxMagnification = +p;
			return options;
		};

		options.scaleNodesByIndividuals = function (p) {
			if (!arguments.length) return scaleNodesByIndividuals;
			scaleNodesByIndividuals = p;
			return options;
		};

		options.selectionModules = function (p) {
			if (!arguments.length) return selectionModules;
			selectionModules = p;
			return options;
		};

		options.width = function (p) {
			if (!arguments.length) return width;
			width = +p;
			return options;
		};

		return options;
	};


/***/ },
/* 56 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {var OwlThing = __webpack_require__(24);
	var RdfsLiteral = __webpack_require__(31);
	var OwlDisjointWith = __webpack_require__(36);
	var attributeParser = __webpack_require__(57)();
	var elementTools = __webpack_require__(54)();
	var nodePrototypeMap = __webpack_require__(5)();
	var propertyPrototypeMap = __webpack_require__(33)();

	/**
	 * Encapsulates the parsing and preparation logic of the input data.
	 * @param graph the graph object that will be passed to the elements
	 * @returns {{}}
	 */
	module.exports = function (graph) {
		var parser = {},
			nodes,
			properties,
			classMap,
			propertyMap;

		/**
		 * Parses the ontology data and preprocesses it (e.g. connecting inverse properties and so on).
		 * @param ontologyData the loaded ontology json file
		 */
		parser.parse = function (ontologyData) {
			if (!ontologyData) {
				nodes = [];
				properties = [];
				return;
			}

			var classes = combineClasses(ontologyData.class, ontologyData.classAttribute),
				datatypes = combineClasses(ontologyData.datatype, ontologyData.datatypeAttribute),
				combinedClassesAndDatatypes = classes.concat(datatypes),
				combinedProperties;

			// Inject properties for unions, intersections, ...
			addSetOperatorProperties(combinedClassesAndDatatypes, ontologyData.property);

			combinedProperties = combineProperties(ontologyData.property, ontologyData.propertyAttribute);

			classMap = mapElements(combinedClassesAndDatatypes);
			propertyMap = mapElements(combinedProperties);

			mergeRangesOfEquivalentProperties(combinedProperties, combinedClassesAndDatatypes);

			// Process the graph data
			convertTypesToIris(combinedClassesAndDatatypes, ontologyData.namespace);
			convertTypesToIris(combinedProperties, ontologyData.namespace);

			nodes = createNodeStructure(combinedClassesAndDatatypes, classMap);
			properties = createPropertyStructure(combinedProperties, classMap, propertyMap);
		};

		/**
		 * @return {Array} the preprocessed nodes
		 */
		parser.nodes = function () {
			return nodes;
		};

		/**
		 * @returns {Array} the preprocessed properties
		 */
		parser.properties = function () {
			return properties;
		};

		/**
		 * Combines the passed objects with its attributes and prototypes. This also applies
		 * attributes defined in the base of the prototype.
		 */
		function combineClasses(baseObjects, attributes) {
			var combinations = [];
			var prototypeMap = createLowerCasePrototypeMap(nodePrototypeMap);

			if (baseObjects) {
				baseObjects.forEach(function (element) {
					var matchingAttribute;

					if (attributes) {
						// Look for an attribute with the same id and merge them
						for (var i = 0; i < attributes.length; i++) {
							var attribute = attributes[i];
							if (element.id === attribute.id) {
								matchingAttribute = attribute;
								break;
							}
						}
						addAdditionalAttributes(element, matchingAttribute);
					}

					// Then look for a prototype to add its properties
					var Prototype = prototypeMap.get(element.type.toLowerCase());

					if (Prototype) {
						addAdditionalAttributes(element, Prototype); // TODO might be unnecessary

						var node = new Prototype(graph);
						node.annotations(element.annotations)
							.comment(element.comment)
							.complement(element.complement)
							.description(element.description)
							.equivalents(element.equivalent)
							.id(element.id)
							.intersection(element.intersection)
							.label(element.label)
							// .type(element.type) Ignore, because we predefined it
							.union(element.union)
							.iri(element.iri);

						// Create node objects for all individuals
						if (element.individuals) {
							element.individuals.forEach(function (individual) {
								var individualNode = new Prototype(graph);
								individualNode.label(individual.labels)
									.iri(individual.iri);

								node.individuals().push(individualNode);
							});
						}

						if (element.attributes) {
							var deduplicatedAttributes = d3.set(element.attributes.concat(node.attributes()));
							node.attributes(deduplicatedAttributes.values());
						}

						combinations.push(node);
					} else {
						console.error("Unknown element type: " + element.type);
					}
				});
			}

			return combinations;
		}

		function combineProperties(baseObjects, attributes) {
			var combinations = [];
			var prototypeMap = createLowerCasePrototypeMap(propertyPrototypeMap);

			if (baseObjects) {
				baseObjects.forEach(function (element) {
					var matchingAttribute;

					if (attributes) {
						// Look for an attribute with the same id and merge them
						for (var i = 0; i < attributes.length; i++) {
							var attribute = attributes[i];
							if (element.id === attribute.id) {
								matchingAttribute = attribute;
								break;
							}
						}
						addAdditionalAttributes(element, matchingAttribute);
					}

					// Then look for a prototype to add its properties
					var Prototype = prototypeMap.get(element.type.toLowerCase());

					if (Prototype) {
						// Create the matching object and set the properties
						var property = new Prototype(graph);
						property.annotations(element.annotations)
							.cardinality(element.cardinality)
							.comment(element.comment)
							.domain(element.domain)
							.description(element.description)
							.equivalents(element.equivalent)
							.id(element.id)
							.inverse(element.inverse)
							.label(element.label)
							.minCardinality(element.minCardinality)
							.maxCardinality(element.maxCardinality)
							.range(element.range)
							.subproperties(element.subproperty)
							.superproperties(element.superproperty)
							// .type(element.type) Ignore, because we predefined it
							.iri(element.iri);

						if (element.attributes) {
							var deduplicatedAttributes = d3.set(element.attributes.concat(property.attributes()));
							property.attributes(deduplicatedAttributes.values());
						}

						combinations.push(property);
					} else {
						console.error("Unknown element type: " + element.type);
					}

				});
			}

			return combinations;
		}

		function createLowerCasePrototypeMap(prototypeMap) {
			return d3.map(prototypeMap.values(), function (Prototype) {
				return new Prototype().type().toLowerCase();
			});
		}

		/**
		 * Really dirty implementation of the range merging of equivalent Ids,
		 * but this will be moved to the converter.
		 * @param properties
		 * @param nodes
		 */
		function mergeRangesOfEquivalentProperties(properties, nodes) {
			var backedUpNodes = nodes.slice(),
				hiddenNodeIds = d3.set(),
				i, l, j, k,
				prefix = "GENERATED-MERGED_RANGE-";

			// clear the original array
			nodes.length = 0;

			for (i = 0, l = properties.length; i < l; i++) {
				var property = properties[i],
					equivalents = property.equivalents();

				if (equivalents.length === 0) {
					continue;
				}

				// quickfix, because all equivalent properties have the equivalent attribute
				if (property.range().indexOf(prefix) === 0) {
					continue;
				}

				var mergedRange;
				if (elementTools.isDatatypeProperty(property)) {
					mergedRange = new RdfsLiteral(graph);
				} else {
					mergedRange = new OwlThing(graph);
				}
				mergedRange.id(prefix + property.id());
				nodes.push(mergedRange);

				var hiddenNodeId = property.range();
				property.range(mergedRange.id());

				for (j = 0, k = equivalents.length; j < k; j++) {
					var equivalentId = equivalents[j],
						equivProperty = propertyMap[equivalentId];

					var oldRange = equivProperty.range();
					equivProperty.range(mergedRange.id());
					if (!isDomainOrRangeOfOtherProperty(oldRange, properties)) {
						hiddenNodeIds.add(oldRange);
					}
				}

				// only merge if this property was the only connected one
				if (!isDomainOrRangeOfOtherProperty(hiddenNodeId, properties)) {
					hiddenNodeIds.add(hiddenNodeId);
				}
			}

			for (i = 0, l = backedUpNodes.length; i < l; i++) {
				var node = backedUpNodes[i];

				if (!hiddenNodeIds.has(node.id())) {
					nodes.push(node);
				}
			}

			// Create a map again
			classMap = mapElements(nodes);
		}

		function isDomainOrRangeOfOtherProperty(nodeId, properties) {
			var i, l;

			for (i = 0, l = properties.length; i < l; i++) {
				var property = properties[i];
				if (property.domain() === nodeId || property.range() === nodeId) {
					return true;
				}
			}

			return false;
		}

		/**
		 * Checks all attributes which have to be rewritten.
		 * For example:
		 * <b>equivalent</b> is filled with only ID's of the corresponding nodes. It would be better to used the
		 * object instead of the ID so we swap the ID's with the correct object reference and can delete it from drawing
		 * because it is not necessary.
		 */
		function createNodeStructure(rawNodes, classMap) {
			var nodes = [];

			// Set the default values
			var maxIndividualCount = 0;
			rawNodes.forEach(function (node) {
				maxIndividualCount = Math.max(maxIndividualCount, node.individuals().length);
				node.visible(true);
			});

			rawNodes.forEach(function (node) {
				// Merge and connect the equivalent nodes
				processEquivalentIds(node, classMap);

				attributeParser.parseClassAttributes(node);

				node.maxIndividualCount(maxIndividualCount);
			});

			// Collect all nodes that should be displayed
			rawNodes.forEach(function (node) {
				if (node.visible()) {
					nodes.push(node);
				}
			});

			return nodes;
		}

		/**
		 * Sets the disjoint attribute of the nodes if a disjoint label is found.
		 * @param property
		 */
		function processDisjoints(property) {
			if (property instanceof OwlDisjointWith === false) {
				return;
			}

			var domain = property.domain(),
				range = property.range();

			// Check the domain.
			if (!domain.disjointWith()) {
				domain.disjointWith([]);
			}

			// Check the range.
			if (!range.disjointWith()) {
				range.disjointWith([]);
			}

			domain.disjointWith().push(property.range());
			range.disjointWith().push(property.domain());
		}

		/**
		 * Connect all properties and also their sub- and superproperties.
		 * We iterate over the rawProperties array because it is way faster than iterating
		 * over an object and its attributes.
		 *
		 * @param rawProperties the properties
		 * @param classMap a map of all classes
		 * @param propertyMap the properties in a map
		 */
		function createPropertyStructure(rawProperties, classMap, propertyMap) {
			var properties = [];

			// Set default values
			rawProperties.forEach(function (property) {
				property.visible(true);
			});

			// Connect properties
			rawProperties.forEach(function (property) {
				var domain,
					range,
					domainObject,
					rangeObject,
					inverse;

				/* Skip properties that have no information about their domain and range, like
				 inverse properties with optional inverse and optional domain and range attributes */
				if ((property.domain() && property.range()) || property.inverse()) {

					var inversePropertyId = findId(property.inverse());
					// Look if an inverse property exists
					if (inversePropertyId) {
						inverse = propertyMap[inversePropertyId];
						if (!inverse) {
							console.warn("No inverse property was found for id: " + inversePropertyId);
						}
					}

					// Either domain and range are set on this property or at the inverse
					if (typeof property.domain() !== "undefined" && typeof property.range() !== "undefined") {
						domain = findId(property.domain());
						range = findId(property.range());

						domainObject = classMap[domain];
						rangeObject = classMap[range];
					} else if (inverse) {
						// Domain and range need to be switched
						domain = findId(inverse.range());
						range = findId(inverse.domain());

						domainObject = classMap[domain];
						rangeObject = classMap[range];
					} else {
						console.warn("Domain and range not found for property: " + property.id());
					}

					// Set the references on this property
					property.domain(domainObject);
					property.range(rangeObject);

					// Also set the attributes of the inverse property
					if (inverse) {
						property.inverse(inverse);
						inverse.inverse(property);

						// Switch domain and range
						inverse.domain(rangeObject);
						inverse.range(domainObject);
					}
				}

				// Reference sub- and superproperties
				referenceSubOrSuperProperties(property.subproperties());
				referenceSubOrSuperProperties(property.superproperties());
			});

			// Merge equivalent properties and process disjoints.
			rawProperties.forEach(function (property) {
				processEquivalentIds(property, propertyMap);
				processDisjoints(property);

				attributeParser.parsePropertyAttributes(property);
			});

			// Add additional information to the properties
			rawProperties.forEach(function (property) {

				// Properties of merged classes should point to/from the visible equivalent class
				var propertyWasRerouted = false;
				if (wasNodeMerged(property.domain())) {
					property.domain(property.domain().equivalentBase());
					propertyWasRerouted = true;
				}
				if (wasNodeMerged(property.range())) {
					property.range(property.range().equivalentBase());
					propertyWasRerouted = true;
				}

				// But there should not be two equal properties between the same domain and range
				var equalProperty = getOtherEqualProperty(rawProperties, property);
				if (propertyWasRerouted && equalProperty) {
					property.visible(false);

					equalProperty.redundantProperties().push(property);
				}

				// Hide property if source or target node is hidden
				if (!property.domain().visible() || !property.range().visible()) {
					property.visible(false);
				}

				// Collect all properties that should be displayed
				if (property.visible()) {
					properties.push(property);
				}
			});

			return properties;
		}

		function referenceSubOrSuperProperties(subOrSuperPropertiesArray) {
			var i, l;

			if (!subOrSuperPropertiesArray) {
				return;
			}

			for (i = 0, l = subOrSuperPropertiesArray.length; i < l; ++i) {
				var subOrSuperPropertyId = findId(subOrSuperPropertiesArray[i]);
				var subOrSuperProperty = propertyMap[subOrSuperPropertyId];

				if (subOrSuperProperty) {
					// Replace id with object
					subOrSuperPropertiesArray[i] = subOrSuperProperty;
				} else {
					console.warn("No sub-/superproperty was found for id: " + subOrSuperPropertyId);
				}
			}
		}

		function wasNodeMerged(node) {
			return !node.visible() && node.equivalentBase();
		}

		function getOtherEqualProperty(properties, referenceProperty) {
			var i, l, property;

			for (i = 0, l = properties.length; i < l; i++) {
				property = properties[i];

				if (referenceProperty === property) {
					continue;
				}
				if (referenceProperty.domain() !== property.domain() ||
					referenceProperty.range() !== property.range()) {
					continue;
				}

				// Check for an equal IRI, if non existent compare label and type
				if (referenceProperty.iri() && property.iri()) {
					if (referenceProperty.iri() === property.iri()) {
						return property;
					}
				} else if (referenceProperty.type() === property.type() &&
					referenceProperty.defaultLabel() === property.defaultLabel()) {
					return property;
				}
			}

			return undefined;
		}

		/**
		 * Generates and adds properties for links to set operators.
		 * @param classes unprocessed classes
		 * @param properties unprocessed properties
		 */
		function addSetOperatorProperties(classes, properties) {
			function addProperties(domainId, rangeIds, operatorType) {
				if (!rangeIds) {
					return;
				}

				rangeIds.forEach(function (rangeId, index) {
					var property = {
						id: "GENERATED-" + operatorType + "-" + domainId + "-" + rangeId + "-" + index,
						type: "setOperatorProperty",
						domain: domainId,
						range: rangeId
					};

					properties.push(property);
				});
			}

			classes.forEach(function (clss) {
				addProperties(clss.id(), clss.complement(), "COMPLEMENT");
				addProperties(clss.id(), clss.intersection(), "INTERSECTION");
				addProperties(clss.id(), clss.union(), "UNION");
			});
		}

		/**
		 * Replaces the ids of equivalent nodes/properties with the matching objects, cross references them
		 * and tags them as processed.
		 * @param element a node or a property
		 * @param elementMap a map where nodes/properties can be looked up
		 */
		function processEquivalentIds(element, elementMap) {
			var eqIds = element.equivalents();

			if (!eqIds || element.equivalentBase()) {
				return;
			}

			// Replace ids with the corresponding objects
			for (var i = 0, l = eqIds.length; i < l; ++i) {
				var eqId = findId(eqIds[i]);
				var eqObject = elementMap[eqId];

				if (eqObject) {
					// Cross reference both objects
					eqObject.equivalents(eqObject.equivalents());
					eqObject.equivalents().push(element);
					eqObject.equivalentBase(element);
					eqIds[i] = eqObject;

					// Hide other equivalent nodes
					eqObject.visible(false);
				} else {
					console.warn("No class/property was found for equivalent id: " + eqId);
				}
			}
		}

		/**
		 * Tries to convert the type to an iri and sets it.
		 * @param elements classes or properties
		 * @param namespaces an array of namespaces
		 */
		function convertTypesToIris(elements, namespaces) {
			elements.forEach(function (element) {
				if (typeof element.iri() === "string") {
					element.iri(replaceNamespace(element.iri(), namespaces));
				}
			});
		}

		/**
		 * Creates a map by mapping the array with the passed function.
		 * @param array the array
		 * @returns {{}}
		 */
		function mapElements(array) {
			var map = {};
			for (var i = 0, length = array.length; i < length; i++) {
				var element = array[i];
				map[element.id()] = element;
			}
			return map;
		}

		/**
		 * Adds the attributes of the additional object to the base object, but doesn't
		 * overwrite existing ones.
		 *
		 * @param base the base object
		 * @param addition the object with additional data
		 * @returns the combination is also returned
		 */
		function addAdditionalAttributes(base, addition) {
			// Check for an undefined value
			addition = addition || {};

			for (var addAttribute in addition) {
				// Add the attribute if it doesn't exist
				if (!(addAttribute in base) && addition.hasOwnProperty(addAttribute)) {
					base[addAttribute] = addition[addAttribute];
				}
			}
			return base;
		}

		/**
		 * Replaces the namespace (and the separator) if one exists and returns the new value.
		 * @param address the address with a namespace in it
		 * @param namespaces an array of namespaces
		 * @returns {string} the processed address with the (possibly) replaced namespace
		 */
		function replaceNamespace(address, namespaces) {
			var separatorIndex = address.indexOf(":");
			if (separatorIndex === -1) {
				return address;
			}

			var namespaceName = address.substring(0, separatorIndex);

			for (var i = 0, length = namespaces.length; i < length; ++i) {
				var namespace = namespaces[i];
				if (namespaceName === namespace.name) {
					return namespace.iri + address.substring(separatorIndex + 1);
				}
			}

			return address;
		}

		/**
		 * Looks whether the passed object is already the id or if it was replaced
		 * with the object that belongs to the id.
		 * @param object an id, a class or a property
		 * @returns {string} the id of the passed object or undefined
		 */
		function findId(object) {
			if (!object) {
				return undefined;
			} else if (typeof object === "string") {
				return object;
			} else if ("id" in object) {
				return object.id();
			} else {
				console.warn("No Id was found for this object: " + object);
				return undefined;
			}
		}

		return parser;
	};

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 57 */
/***/ function(module, exports) {

	/**
	 * Parses the attributes an element has and sets the corresponding attributes.
	 * @returns {Function}
	 */
	module.exports = (function () {
		var attributeParser = {},
		// Style
			DEPRECATED = "deprecated",
			EXTERNAL = "external",
			DATATYPE = "datatype",
			OBJECT = "object",
			RDF = "rdf",
		// Representations
			FUNCTIONAL = "functional",
			INVERSE_FUNCTIONAL = "inverse functional",
			TRANSITIVE = "transitive",
			SYMMETRIC = "symmetric";

		/**
		 * Parses and sets the attributes of a class.
		 * @param clazz
		 */
		attributeParser.parseClassAttributes = function (clazz) {
			if (!(clazz.attributes() instanceof Array)) {
				return;
			}

			parseVisualAttributes(clazz);
			parseClassIndications(clazz);
		};

		function parseVisualAttributes(element) {
			var orderedAttributes = [DEPRECATED, EXTERNAL, DATATYPE, OBJECT, RDF],
				i, l, attribute;

			for (i = 0, l = orderedAttributes.length; i < l; i++) {
				attribute = orderedAttributes[i];
				if (element.attributes().indexOf(attribute) >= 0) {
					element.visualAttribute(attribute);

					// Just a single attribute is possible
					break;
				}
			}
		}

		function parseClassIndications(clazz) {
			var indications = [DEPRECATED, EXTERNAL],
				i, l, indication;

			for (i = 0, l = indications.length; i < l; i++) {
				indication = indications[i];

				if (clazz.attributes().indexOf(indication) >= 0) {
					clazz.indications().push(indication);
				}
			}
		}

		/**
		 * Parses and sets the attributes of a property.
		 * @param property
		 */
		attributeParser.parsePropertyAttributes = function (property) {
			if (!(property.attributes() instanceof Array)) {
				return;
			}

			parseVisualAttributes(property);
			parsePropertyIndications(property);
		};

		function parsePropertyIndications(property) {
			var indications = [FUNCTIONAL, INVERSE_FUNCTIONAL, SYMMETRIC, TRANSITIVE],
				i, l, indication;

			for (i = 0, l = indications.length; i < l; i++) {
				indication = indications[i];

				if (property.attributes().indexOf(indication) >= 0) {
					property.indications().push(indication);
				}
			}
		}


		return function () {
			// Return a function to keep module interfaces consistent
			return attributeParser;
		};
	})();


/***/ },
/* 58 */
/***/ function(module, exports) {

	/**
	 * This module abuses the filter function a bit like the statistics module. Nothing is filtered.
	 *
	 * @returns {{}}
	 */
	module.exports = function (graph) {

		var DEFAULT_STATE = false;

		var filter = {},
			nodes,
			properties,
			enabled = DEFAULT_STATE,
			filteredNodes,
			filteredProperties;


		/**
		 * If enabled, redundant details won't be drawn anymore.
		 * @param untouchedNodes
		 * @param untouchedProperties
		 */
		filter.filter = function (untouchedNodes, untouchedProperties) {
			nodes = untouchedNodes;
			properties = untouchedProperties;

			graph.options().compactNotation(enabled);

			filteredNodes = nodes;
			filteredProperties = properties;
		};

		filter.enabled = function (p) {
			if (!arguments.length) return enabled;
			enabled = p;
			return filter;
		};

		filter.reset = function () {
			enabled = DEFAULT_STATE;
		};


		// Functions a filter must have
		filter.filteredNodes = function () {
			return filteredNodes;
		};

		filter.filteredProperties = function () {
			return filteredProperties;
		};


		return filter;
	};


/***/ },
/* 59 */
/***/ function(module, exports, __webpack_require__) {

	var elementTools = __webpack_require__(54)();
	var filterTools = __webpack_require__(60)();

	module.exports = function () {

		var filter = {},
			nodes,
			properties,
			enabled = false,
			filteredNodes,
			filteredProperties;


		/**
		 * If enabled, all datatypes and literals including connected properties are filtered.
		 * @param untouchedNodes
		 * @param untouchedProperties
		 */
		filter.filter = function (untouchedNodes, untouchedProperties) {
			nodes = untouchedNodes;
			properties = untouchedProperties;

			if (this.enabled()) {
				removeDatatypesAndLiterals();
			}

			filteredNodes = nodes;
			filteredProperties = properties;
		};

		function removeDatatypesAndLiterals() {
			var filteredData = filterTools.filterNodesAndTidy(nodes, properties, isNoDatatypeOrLiteral);

			nodes = filteredData.nodes;
			properties = filteredData.properties;
		}

		function isNoDatatypeOrLiteral(node) {
			return !elementTools.isDatatype(node);
		}

		filter.enabled = function (p) {
			if (!arguments.length) return enabled;
			enabled = p;
			return filter;
		};


		// Functions a filter must have
		filter.filteredNodes = function () {
			return filteredNodes;
		};

		filter.filteredProperties = function () {
			return filteredProperties;
		};


		return filter;
	};


/***/ },
/* 60 */
/***/ function(module, exports, __webpack_require__) {

	var elementTools = __webpack_require__(54)();

	module.exports = (function () {

		var tools = {};

		/**
		 * Filters the passed nodes and removes dangling properties.
		 * @param nodes
		 * @param properties
		 * @param shouldKeepNode function that returns true if the node should be kept
		 * @returns {{nodes: Array, properties: Array}} the filtered nodes and properties
		 */
		tools.filterNodesAndTidy = function (nodes, properties, shouldKeepNode) {
			var removedNodes = __webpack_require__(50)(),
				cleanedNodes = [],
				cleanedProperties = [];

			nodes.forEach(function (node) {
				if (shouldKeepNode(node)) {
					cleanedNodes.push(node);
				} else {
					removedNodes.add(node);
				}
			});

			properties.forEach(function (property) {
				if (propertyHasVisibleNodes(removedNodes, property)) {
					cleanedProperties.push(property);
				} else if (elementTools.isDatatypeProperty(property)) {
					// Remove floating datatypes/literals, because they belong to their datatype property
					var index = cleanedNodes.indexOf(property.range());
					if (index >= 0) {
						cleanedNodes.splice(index, 1);
					}
				}
			});

			return {
				nodes: cleanedNodes,
				properties: cleanedProperties
			};
		};

		/**
		 * Returns true, if the domain and the range of this property have not been removed.
		 * @param removedNodes
		 * @param property
		 * @returns {boolean} true if property isn't dangling
		 */
		function propertyHasVisibleNodes(removedNodes, property) {
			return !removedNodes.has(property.domain()) && !removedNodes.has(property.range());
		}


		return function () {
			return tools;
		};
	})();


/***/ },
/* 61 */
/***/ function(module, exports, __webpack_require__) {

	var OwlDisjointWith = __webpack_require__(36);

	module.exports = function () {

		var filter = {},
			nodes,
			properties,
		// According to the specification enabled by default
			enabled = true,
			filteredNodes,
			filteredProperties;


		/**
		 * If enabled, all disjoint with properties are filtered.
		 * @param untouchedNodes
		 * @param untouchedProperties
		 */
		filter.filter = function (untouchedNodes, untouchedProperties) {
			nodes = untouchedNodes;
			properties = untouchedProperties;

			if (this.enabled()) {
				removeDisjointWithProperties();
			}

			filteredNodes = nodes;
			filteredProperties = properties;
		};

		function removeDisjointWithProperties() {
			var cleanedProperties = [],
				i, l, property;

			for (i = 0, l = properties.length; i < l; i++) {
				property = properties[i];

				if (!(property instanceof OwlDisjointWith)) {
					cleanedProperties.push(property);
				}
			}

			properties = cleanedProperties;
		}

		filter.enabled = function (p) {
			if (!arguments.length) return enabled;
			enabled = p;
			return filter;
		};


		// Functions a filter must have
		filter.filteredNodes = function () {
			return filteredNodes;
		};

		filter.filteredProperties = function () {
			return filteredProperties;
		};


		return filter;
	};


/***/ },
/* 62 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {module.exports = function () {
		var focuser = {},
			focusedElement;

		focuser.handle = function (selectedElement) {
			// Don't display details on a drag event, which will be prevented
			if (d3.event.defaultPrevented) {
				return;
			}

			if (focusedElement !== undefined) {
				focusedElement.toggleFocus();
			}

			if (focusedElement !== selectedElement) {
				selectedElement.toggleFocus();
				focusedElement = selectedElement;
			} else {
				focusedElement = undefined;
			}
		};

		/**
		 * Removes the focus if an element is focussed.
		 */
		focuser.reset = function () {
			if (focusedElement) {
				focusedElement.toggleFocus();
				focusedElement = undefined;
			}
		};

		return focuser;
	};

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 63 */
/***/ function(module, exports, __webpack_require__) {

	var elementTools = __webpack_require__(54)();
	var filterTools = __webpack_require__(60)();

	module.exports = function () {

		var filter = {},
			nodes,
			properties,
			enabled = true,
			filteredNodes,
			filteredProperties,
			maxDegreeSetter,
			degreeQueryFunction;


		/**
		 * If enabled, all nodes are filter by their node degree.
		 * @param untouchedNodes
		 * @param untouchedProperties
		 */
		filter.filter = function (untouchedNodes, untouchedProperties) {
			nodes = untouchedNodes;
			properties = untouchedProperties;

			setMaxLinkCount();

			if (this.enabled()) {
				if (degreeQueryFunction instanceof Function) {
					filterByNodeDegree(degreeQueryFunction());
				} else {
					console.error("No degree query function set.");
				}
			}

			filteredNodes = nodes;
			filteredProperties = properties;
		};

		function setMaxLinkCount() {
			var maxLinkCount = 0;
			for (var i = 0, l = nodes.length; i < l; i++) {
				var linksWithoutDatatypes = filterOutDatatypes(nodes[i].links());

				maxLinkCount = Math.max(maxLinkCount, linksWithoutDatatypes.length);
			}

			if (maxDegreeSetter instanceof Function) {
				maxDegreeSetter(maxLinkCount);
			}
		}

		function filterOutDatatypes(links) {
			return links.filter(function (link) {
				return !elementTools.isDatatypeProperty(link.property());
			});
		}

		function filterByNodeDegree(minDegree) {
			var filteredData = filterTools.filterNodesAndTidy(nodes, properties, hasRequiredDegree(minDegree));

			nodes = filteredData.nodes;
			properties = filteredData.properties;
		}

		function hasRequiredDegree(minDegree) {
			return function (node) {
				return filterOutDatatypes(node.links()).length >= minDegree;
			};
		}

		filter.setMaxDegreeSetter = function (maxNodeDegreeSetter) {
			maxDegreeSetter = maxNodeDegreeSetter;
		};

		filter.setDegreeQueryFunction = function (nodeDegreeQueryFunction) {
			degreeQueryFunction = nodeDegreeQueryFunction;
		};

		filter.enabled = function (p) {
			if (!arguments.length) return enabled;
			enabled = p;
			return filter;
		};


		// Functions a filter must have
		filter.filteredNodes = function () {
			return filteredNodes;
		};

		filter.filteredProperties = function () {
			return filteredProperties;
		};


		return filter;
	};


/***/ },
/* 64 */
/***/ function(module, exports) {

	/**
	 * This module abuses the filter function a bit like the statistics module. Nothing is filtered.
	 *
	 * @returns {{}}
	 */
	module.exports = function (graph) {

		var DEFAULT_STATE = true;

		var filter = {},
			nodes,
			properties,
			enabled = DEFAULT_STATE,
			filteredNodes,
			filteredProperties;


		/**
		 * If enabled, the scaling of nodes according to individuals will be enabled.
		 * @param untouchedNodes
		 * @param untouchedProperties
		 */
		filter.filter = function (untouchedNodes, untouchedProperties) {
			nodes = untouchedNodes;
			properties = untouchedProperties;

			graph.options().scaleNodesByIndividuals(enabled);

			filteredNodes = nodes;
			filteredProperties = properties;
		};

		filter.enabled = function (p) {
			if (!arguments.length) return enabled;
			enabled = p;
			return filter;
		};

		filter.reset = function () {
			enabled = DEFAULT_STATE;
		};


		// Functions a filter must have
		filter.filteredNodes = function () {
			return filteredNodes;
		};

		filter.filteredProperties = function () {
			return filteredProperties;
		};


		return filter;
	};


/***/ },
/* 65 */
/***/ function(module, exports, __webpack_require__) {

	var elementTools = __webpack_require__(54)();

	module.exports = function () {
		var pap = {},
			enabled = false,
			pinnedNodes = [];

		pap.handle = function (selectedElement) {
			if (!enabled) {
				return;
			}

			if (!elementTools.isDatatype(selectedElement) && !selectedElement.pinned()) {
				selectedElement.drawPin();
				pinnedNodes.push(selectedElement);
			}
		};

		pap.enabled = function (p) {
			if (!arguments.length) return enabled;
			enabled = p;
			return pap;
		};

		pap.reset = function () {
			var i = 0, l = pinnedNodes.length;
			for (; i < l; i++) {
				pinnedNodes[i].removePin();
			}
			// Clear the array of stored nodes
			pinnedNodes.length = 0;
		};

		return pap;
	};


/***/ },
/* 66 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {module.exports = function (handlerFunction) {
		var viewer = {},
			lastSelectedElement;

		viewer.handle = function (selectedElement) {
			// Don't display details on a drag event, which will be prevented
			if (d3.event.defaultPrevented) {
				return;
			}

			var isSelection = true;

			// Deselection of the focused element
			if (lastSelectedElement === selectedElement) {
				isSelection = false;
			}

			if (handlerFunction instanceof Function) {
				if (isSelection) {
					handlerFunction(selectedElement);
				} else {
					handlerFunction(undefined);
				}
			}

			if (isSelection) {
				lastSelectedElement = selectedElement;
			} else {
				lastSelectedElement = undefined;
			}
		};

		/**
		 * Resets the displayed information to its default.
		 */
		viewer.reset = function () {
			if (lastSelectedElement) {
				handlerFunction(undefined);
				lastSelectedElement = undefined;
			}
		};

		return viewer;
	};

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 67 */
/***/ function(module, exports, __webpack_require__) {

	var SetOperatorNode = __webpack_require__(19);

	module.exports = function () {

		var filter = {},
			nodes,
			properties,
			enabled = false,
			filteredNodes,
			filteredProperties,
			filterTools = __webpack_require__(60)();


		/**
		 * If enabled, all set operators including connected properties are filtered.
		 * @param untouchedNodes
		 * @param untouchedProperties
		 */
		filter.filter = function (untouchedNodes, untouchedProperties) {
			nodes = untouchedNodes;
			properties = untouchedProperties;

			if (this.enabled()) {
				removeSetOperators();
			}

			filteredNodes = nodes;
			filteredProperties = properties;
		};

		function removeSetOperators() {
			var filteredData = filterTools.filterNodesAndTidy(nodes, properties, isNoSetOperator);

			nodes = filteredData.nodes;
			properties = filteredData.properties;
		}

		function isNoSetOperator(node) {
			return !(node instanceof SetOperatorNode);
		}

		filter.enabled = function (p) {
			if (!arguments.length) return enabled;
			enabled = p;
			return filter;
		};


		// Functions a filter must have
		filter.filteredNodes = function () {
			return filteredNodes;
		};

		filter.filteredProperties = function () {
			return filteredProperties;
		};


		return filter;
	};


/***/ },
/* 68 */
/***/ function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(d3) {var SetOperatorNode = __webpack_require__(19);
	var OwlThing = __webpack_require__(24);
	var OwlNothing = __webpack_require__(23);
	var elementTools = __webpack_require__(54)();

	module.exports = function () {

		var statistics = {},
			nodeCount,
			occurencesOfClassAndDatatypeTypes = {},
			edgeCount,
			occurencesOfPropertyTypes = {},
			classCount,
			datatypeCount,
			datatypePropertyCount,
			objectPropertyCount,
			propertyCount,
			totalIndividualCount,
			filteredNodes,
			filteredProperties;


		statistics.filter = function (classesAndDatatypes, properties) {
			resetStoredData();

			storeTotalCounts(classesAndDatatypes, properties);
			storeClassAndDatatypeCount(classesAndDatatypes);
			storePropertyCount(properties);

			storeOccurencesOfTypes(classesAndDatatypes, occurencesOfClassAndDatatypeTypes);
			storeOccurencesOfTypes(properties, occurencesOfPropertyTypes);

			storeTotalIndividualCount(classesAndDatatypes);

			filteredNodes = classesAndDatatypes;
			filteredProperties = properties;
		};

		function resetStoredData() {
			nodeCount = 0;
			edgeCount = 0;
			classCount = 0;
			datatypeCount = 0;
			datatypePropertyCount = 0;
			objectPropertyCount = 0;
			propertyCount = 0;
			totalIndividualCount = 0;
		}

		function storeTotalCounts(classesAndDatatypes, properties) {
			nodeCount = classesAndDatatypes.length;

			var seenProperties = __webpack_require__(50)(), i, l, property;
			for (i = 0, l = properties.length; i < l; i++) {
				property = properties[i];
				if (!seenProperties.has(property)) {
					edgeCount += 1;
				}

				seenProperties.add(property);
				if (property.inverse()) {
					seenProperties.add(property.inverse());
				}
			}
		}

		function storeClassAndDatatypeCount(classesAndDatatypes) {
			// Each datatype should be counted just a single time
			var datatypeSet = d3.set(),
				hasThing = false,
				hasNothing = false;

			classesAndDatatypes.forEach(function (node) {
				if (elementTools.isDatatype(node)) {
					datatypeSet.add(node.defaultLabel());
				} else if (!(node instanceof SetOperatorNode)) {
					if (node instanceof OwlThing) {
						hasThing = true;
					} else if (node instanceof OwlNothing) {
						hasNothing = true;
					} else {
						classCount += 1;
						classCount += countElementArray(node.equivalents());
					}
				}
			});

			// count things and nothings just a single time
			classCount += hasThing ? 1 : 0;
			classCount += hasNothing ? 1 : 0;

			datatypeCount = datatypeSet.size();
		}

		function storePropertyCount(properties) {
			for (var i = 0, l = properties.length; i < l; i++) {
				var property = properties[i];

				if (elementTools.isObjectProperty(property)) {
					objectPropertyCount += getExtendedPropertyCount(property);
				} else if (elementTools.isDatatypeProperty(properties)) {
					datatypePropertyCount += getExtendedPropertyCount(property);
				}
			}
			propertyCount = objectPropertyCount + datatypePropertyCount;
		}

		function getExtendedPropertyCount(property) {
			// count the property itself
			var count = 1;

			// and count properties this property represents
			count += countElementArray(property.equivalents());
			count += countElementArray(property.redundantProperties());

			return count;
		}

		function countElementArray(properties) {
			if (properties) {
				return properties.length;
			}
			return 0;
		}

		function storeOccurencesOfTypes(elements, storage) {
			elements.forEach(function (element) {
				var type = element.type(),
					typeCount = storage[type];

				if (typeof typeCount === "undefined") {
					typeCount = 0;
				} else {
					typeCount += 1;
				}
				storage[type] = typeCount;
			});
		}

		function storeTotalIndividualCount(nodes) {
			var totalCount = 0;
			for (var i = 0, l = nodes.length; i < l; i++) {
				totalCount += nodes[i].individuals().length || 0;
			}
			totalIndividualCount = totalCount;
		}


		statistics.nodeCount = function () {
			return nodeCount;
		};

		statistics.occurencesOfClassAndDatatypeTypes = function () {
			return occurencesOfClassAndDatatypeTypes;
		};

		statistics.edgeCount = function () {
			return edgeCount;
		};

		statistics.occurencesOfPropertyTypes = function () {
			return occurencesOfPropertyTypes;
		};

		statistics.classCount = function () {
			return classCount;
		};

		statistics.datatypeCount = function () {
			return datatypeCount;
		};

		statistics.datatypePropertyCount = function () {
			return datatypePropertyCount;
		};

		statistics.objectPropertyCount = function () {
			return objectPropertyCount;
		};

		statistics.propertyCount = function () {
			return propertyCount;
		};

		statistics.totalIndividualCount = function () {
			return totalIndividualCount;
		};


		// Functions a filter must have
		statistics.filteredNodes = function () {
			return filteredNodes;
		};

		statistics.filteredProperties = function () {
			return filteredProperties;
		};


		return statistics;
	};

	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ },
/* 69 */
/***/ function(module, exports, __webpack_require__) {

	var elementTools = __webpack_require__(54)();

	module.exports = function () {

		var filter = {},
			nodes,
			properties,
			enabled = false,
			filteredNodes,
			filteredProperties;


		/**
		 * If enabled subclasses that have only subclass properties are filtered.
		 * @param untouchedNodes
		 * @param untouchedProperties
		 */
		filter.filter = function (untouchedNodes, untouchedProperties) {
			nodes = untouchedNodes;
			properties = untouchedProperties;

			if (this.enabled()) {
				hideSubclassesWithoutOwnProperties();
			}

			filteredNodes = nodes;
			filteredProperties = properties;
		};

		function hideSubclassesWithoutOwnProperties() {
			var unneededProperties = [],
				unneededClasses = [],
				subclasses = [],
				connectedProperties,
				subclass,
				property,
				i, // index,
				l; // length


			for (i = 0, l = properties.length; i < l; i++) {
				property = properties[i];
				if (elementTools.isRdfsSubClassOf(property)) {
					subclasses.push(property.domain());
				}
			}

			for (i = 0, l = subclasses.length; i < l; i++) {
				subclass = subclasses[i];
				connectedProperties = findRelevantConnectedProperties(subclass, properties);

				// Only remove the node and its properties, if they're all subclassOf properties
				if (areOnlySubclassProperties(connectedProperties) &&
					doesNotInheritFromMultipleClasses(subclass, connectedProperties)) {

					unneededProperties = unneededProperties.concat(connectedProperties);
					unneededClasses.push(subclass);
				}
			}

			nodes = removeUnneededElements(nodes, unneededClasses);
			properties = removeUnneededElements(properties, unneededProperties);
		}

		/**
		 * Looks recursively for connected properties. Because just subclasses are relevant,
		 * we just look recursively for their properties.
		 *
		 * @param node
		 * @param allProperties
		 * @param visitedNodes a visited nodes which is used on recursive invocation
		 * @returns {Array}
		 */
		function findRelevantConnectedProperties(node, allProperties, visitedNodes) {
			var connectedProperties = [],
				property,
				i,
				l;

			for (i = 0, l = allProperties.length; i < l; i++) {
				property = allProperties[i];
				if (property.domain() === node ||
					property.range() === node) {

					connectedProperties.push(property);


					/* Special case: SuperClass <-(1) Subclass <-(2) Subclass ->(3) e.g. Datatype
					 * We need to find the last property recursively. Otherwise, we would remove the subClassOf
					 * property (1) because we didn't see the datatype property (3).
					 */

					// Look only for subclass properties, because these are the relevant properties
					if (elementTools.isRdfsSubClassOf(property)) {
						var domain = property.domain();
						visitedNodes = visitedNodes || __webpack_require__(50)();

						// If we have the range, there might be a nested property on the domain
						if (node === property.range() && !visitedNodes.has(domain)) {
							visitedNodes.add(domain);
							var nestedConnectedProperties = findRelevantConnectedProperties(domain, allProperties, visitedNodes);
							connectedProperties = connectedProperties.concat(nestedConnectedProperties);
						}
					}
				}
			}

			return connectedProperties;
		}

		function areOnlySubclassProperties(connectedProperties) {
			var onlySubclassProperties = true,
				property,
				i,
				l;

			for (i = 0, l = connectedProperties.length; i < l; i++) {
				property = connectedProperties[i];

				if (!elementTools.isRdfsSubClassOf(property)) {
					onlySubclassProperties = false;
					break;
				}
			}

			return onlySubclassProperties;
		}

		function doesNotInheritFromMultipleClasses(subclass, connectedProperties) {
			var superClassCount = 0;

			for (var i = 0, l = connectedProperties.length; i < l; i++) {
				var property = connectedProperties[i];

				if (property.domain() === subclass) {
					superClassCount += 1;
				}

				if (superClassCount > 1) {
					return false;
				}
			}

			return true;
		}

		function removeUnneededElements(array, removableElements) {
			var disjoint = [],
				element,
				i,
				l;

			for (i = 0, l = array.length; i < l; i++) {
				element = array[i];
				if (removableElements.indexOf(element) === -1) {
					disjoint.push(element);
				}
			}
			return disjoint;
		}

		filter.enabled = function (p) {
			if (!arguments.length) return enabled;
			enabled = p;
			return filter;
		};


		// Functions a filter must have
		filter.filteredNodes = function () {
			return filteredNodes;
		};

		filter.filteredProperties = function () {
			return filteredProperties;
		};


		return filter;
	};


/***/ }
/******/ ]);
//# sourceMappingURL=webvowl.js.map
