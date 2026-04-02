import reversePath from 'svg-path-reverse';

export default class Drawflow {
  constructor(container, render = null, parent = null) {
    this.events = {};
    this.container = container;
    this.precanvas = null;
    this.nodeId = 1;
    this.ele_selected = null;
    this.node_selected = null;
    this.drag = false;
    this.reroute = false;
    this.reroute_fix_curvature = false;
    this.curvature = 0.5;
    this.reroute_curvature_start_end = 0.5;
    this.reroute_curvature = 0.5;
    this.reroute_width = 6;
    this.drag_point = false;
    this.editor_selected = false;
    this.connection = false;
    this.connection_ele = null;
    this.connection_selected = null;
    this.canvas_x = 0;
    this.canvas_y = 0;
    this.pos_x = 0;
    this.pos_x_start = 0;
    this.pos_y = 0;
    this.pos_y_start = 0;
    this.mouse_x = 0;
    this.mouse_y = 0;
    this.line_path = 5;
    this.first_click = null;
    this.force_first_input = false;
    this.draggable_inputs = true;
    this.useuuid = true;
    this.parent = parent;

    this.noderegister = {};
    this.render = render;
    this.drawflow = { "drawflow": { "Home": { "data": {} }}};
    // Configurable options
    this.module = 'Home';
    this.editor_mode = 'edit';
    this.zoom = 1;
    this.zoom_max = 1.5;
    this.zoom_min = 0.25;
    this.zoom_value = 0.015;
    this.zoom_last_value = 1;

    // Mobile
    this.evCache = new Array();
    this.prevDiff = -1;
  }

  start () {
    // console.info("Start Drawflow!!");
    this.container.classList.add("parent-drawflow");
    this.container.tabIndex = 0;
    this.precanvas = document.createElement('div');
    this.precanvas.classList.add("drawflow");
    this.container.appendChild(this.precanvas);

    /* Mouse and Touch Actions */
    this.container.addEventListener('mouseup', this.dragEnd.bind(this));
    this.container.addEventListener('mousemove', this.position.bind(this));
    this.container.addEventListener('mousedown', this.click.bind(this) );

    this.container.addEventListener('touchend', this.dragEnd.bind(this));
    this.container.addEventListener('touchmove', this.position.bind(this));
    this.container.addEventListener('touchstart', this.click.bind(this));

    /* Context Menu */
    this.container.addEventListener('contextmenu', this.contextmenu.bind(this));
    /* Delete */
    this.container.addEventListener('keydown', this.key.bind(this));

    /* Zoom Mouse */
    this.container.addEventListener('wheel', this.zoom_enter.bind(this));
    /* Update data Nodes */
    this.container.addEventListener('input', this.updateNodeValue.bind(this));

    this.container.addEventListener('dblclick', this.dblclick.bind(this));
    /* Mobile zoom */
    this.container.onpointerdown = this.pointerdown_handler.bind(this);
    this.container.onpointermove = this.pointermove_handler.bind(this);
    this.container.onpointerup = this.pointerup_handler.bind(this);
    this.container.onpointercancel = this.pointerup_handler.bind(this);
    this.container.onpointerout = this.pointerup_handler.bind(this);
    this.container.onpointerleave = this.pointerup_handler.bind(this);
    this.load();
  }

  /* Mobile zoom */
  pointerdown_handler(ev) {
   this.evCache.push(ev);
  }

  pointermove_handler(ev) {
   for (var i = 0; i < this.evCache.length; i++) {
     if (ev.pointerId == this.evCache[i].pointerId) {
        this.evCache[i] = ev;
     break;
     }
   }

   if (this.evCache.length == 2) {
     // Calculate the distance between the two pointers
     var curDiff = Math.abs(this.evCache[0].clientX - this.evCache[1].clientX);

     if (this.prevDiff > 100) {
       if (curDiff > this.prevDiff) {
         // The distance between the two pointers has increased

         this.zoom_in();
       }
       if (curDiff < this.prevDiff) {
         // The distance between the two pointers has decreased
         this.zoom_out();
       }
     }
     this.prevDiff = curDiff;
   }
  }

  pointerup_handler(ev) {
    this.remove_event(ev);
    if (this.evCache.length < 2) {
      this.prevDiff = -1;
    }
  }
  remove_event(ev) {
   // Remove this event from the target's cache
   for (var i = 0; i < this.evCache.length; i++) {
     if (this.evCache[i].pointerId == ev.pointerId) {
       this.evCache.splice(i, 1);
       break;
     }
   }
  }
  /* End Mobile Zoom */
  load() {
    for (var key in this.drawflow.drawflow[this.module].data) {
      this.addNodeImport(this.drawflow.drawflow[this.module].data[key], this.precanvas);
    }

    for (var key in this.drawflow.drawflow[this.module].data) {
      this.updateConnectionNodes('node-'+key);
    }

    const editor = this.drawflow.drawflow;
    let number = 1;
    Object.keys(editor).map(function(moduleName, index) {
      Object.keys(editor[moduleName].data).map(function(id, index2) {
        if(parseInt(id) >= number) {
          number = parseInt(id)+1;
        }
      });
    });
    this.nodeId = number;
  }

  click(e) {
    this.dispatch('click', e);
    if(this.editor_mode === 'fixed') {
      //return false;
       if(e.target.classList[0] === 'parent-drawflow' || e.target.classList[0] === 'drawflow') {
         this.ele_selected = e.target.closest(".parent-drawflow");
         e.preventDefault();
       } else {
         return false;
       }
    } else if(this.editor_mode === 'view') {
      if(e.target.closest(".drawflow") != null || e.target.matches('.parent-drawflow')) {
        this.ele_selected = e.target.closest(".parent-drawflow");
        e.preventDefault();
      }
    } else {
      this.first_click = e.target;
      this.ele_selected = e.target;
      if(e.button === 0) {
        this.contextmenuDel();
      }

      if(e.target.closest(".drawflow_content_node") != null) {
        this.ele_selected = e.target.closest(".drawflow_content_node").parentElement;
      }
    }
    switch (this.ele_selected.classList[0]) {
      case 'drawflow-node':
        if(this.node_selected != null) {
          this.node_selected.classList.remove("selected");
          if(this.node_selected != this.ele_selected) {
            this.dispatch('nodeUnselected', true);
          }
        }
        if(this.connection_selected != null) {
          this.connection_selected.classList.remove("selected");
          this.connection_selected = null;
        }
        if(this.node_selected != this.ele_selected) {
          this.dispatch('nodeSelected', this.ele_selected.id.slice(5));
        }
        this.node_selected = this.ele_selected;
        this.node_selected.classList.add("selected");
        if(!this.draggable_inputs) {
          if(e.target.tagName !== 'INPUT' && e.target.tagName !== 'TEXTAREA' && e.target.tagName !== 'SELECT' && e.target.hasAttribute('contenteditable') !== true) {
            this.drag = true;
          }
        } else {
          if(e.target.tagName !== 'SELECT') {
            this.drag = true;
          }
        }
        break;
      case 'node_connection_point':
        let nodes = this.container.querySelectorAll(".node_connections_left, .node_connections_right")
        nodes.forEach(n => {
          if (n === e.target) return;
          n.style.visibility = 'hidden'; 
        })
        
        this.connection = true;
        if(this.node_selected != null) {
          this.node_selected.classList.remove("selected");
          this.node_selected = null;
          this.dispatch('nodeUnselected', true);
        }
        if(this.connection_selected != null) {
          this.connection_selected.classList.remove("selected");
          this.connection_selected = null;
        }
        this.drawConnection(e.target);
        break;
      case 'parent-drawflow':
        if(this.node_selected != null) {
          this.node_selected.classList.remove("selected");
          this.node_selected = null;
          this.dispatch('nodeUnselected', true);
        }
        if(this.connection_selected != null) {
          this.connection_selected.classList.remove("selected");
          this.connection_selected = null;
        }
        this.editor_selected = true;
        break;
      case 'drawflow':
        if(this.node_selected != null) {
          this.node_selected.classList.remove("selected");
          this.node_selected = null;
          this.dispatch('nodeUnselected', true);
        }
        if(this.connection_selected != null) {
          this.connection_selected.classList.remove("selected");
          this.connection_selected = null;
        }
        this.editor_selected = true;
        break;
      case 'main-path':
        if(this.node_selected != null) {
          this.node_selected.classList.remove("selected");
          this.node_selected = null;
          this.dispatch('nodeUnselected', true);
        }
        if(this.connection_selected != null) {
          this.connection_selected.classList.remove("selected");
          this.connection_selected = null;
        }
        this.connection_selected = this.ele_selected;
        this.connection_selected.classList.add("selected");
        const listclassConnection = this.connection_selected.parentElement.classList;
        if(listclassConnection.length > 1){
          this.dispatch('connectionSelected', { output_id: listclassConnection[2].slice(14), input_id: listclassConnection[1].slice(13), output_class: listclassConnection[3], input_class: listclassConnection[4] });
          if(this.reroute_fix_curvature) {
            this.connection_selected.parentElement.querySelectorAll(".main-path").forEach((item, i) => {
              item.classList.add("selected");
            });
          }
        }
      break;
      case 'point':
        this.drag_point = true;
        this.ele_selected.classList.add("selected");
      break;
      case 'drawflow-delete':
        if(this.node_selected ) {
          this.removeNodeId(this.node_selected.id);
        }

        if(this.connection_selected) {
          this.removeConnection();
        }

        if(this.node_selected != null) {
          this.node_selected.classList.remove("selected");
          this.node_selected = null;
          this.dispatch('nodeUnselected', true);
        }
        if(this.connection_selected != null) {
          this.connection_selected.classList.remove("selected");
          this.connection_selected = null;
        }

      break;
      default:
    }
    if (e.type === "touchstart") {
      this.pos_x = e.touches[0].clientX;
      this.pos_x_start = e.touches[0].clientX;
      this.pos_y = e.touches[0].clientY;
      this.pos_y_start = e.touches[0].clientY;
      this.mouse_x = e.touches[0].clientX;
      this.mouse_y = e.touches[0].clientY;
    } else {
      this.pos_x = e.clientX;
      this.pos_x_start = e.clientX;
      this.pos_y = e.clientY;
      this.pos_y_start = e.clientY;
    }
    if (['node_connection_point','main-path'].includes(this.ele_selected.classList[0])) {
      e.preventDefault();
    }
    this.dispatch('clickEnd', e);
  }

  position(e) {
    if (e.type === "touchmove") {
      var e_pos_x = e.touches[0].clientX;
      var e_pos_y = e.touches[0].clientY;
    } else {
      var e_pos_x = e.clientX;
      var e_pos_y = e.clientY;
    }

    if(this.connection) {
      this.updateConnection(e_pos_x, e_pos_y);
    }
    if(this.editor_selected) {
      x =  this.canvas_x + (-(this.pos_x - e_pos_x))
      y = this.canvas_y + (-(this.pos_y - e_pos_y))

      this.dispatch('translate', { x: x, y: y});
      this.precanvas.style.transform = "translate("+x+"px, "+y+"px) scale("+this.zoom+")";
    }
    if(this.drag) {
      e.preventDefault();
      var x = (this.pos_x - e_pos_x) * this.precanvas.clientWidth / (this.precanvas.clientWidth * this.zoom);
      var y = (this.pos_y - e_pos_y) * this.precanvas.clientHeight / (this.precanvas.clientHeight * this.zoom);
      this.pos_x = e_pos_x;
      this.pos_y = e_pos_y;

      this.ele_selected.style.top = (this.ele_selected.offsetTop - y) + "px";
      this.ele_selected.style.left = (this.ele_selected.offsetLeft - x) + "px";

      this.drawflow.drawflow[this.module].data[this.ele_selected.id.slice(5)].pos_x = (this.ele_selected.offsetLeft - x);
      this.drawflow.drawflow[this.module].data[this.ele_selected.id.slice(5)].pos_y = (this.ele_selected.offsetTop - y);
      this.updateConnectionNodes(this.ele_selected.id)
    }

    if (e.type === "touchmove") {
      this.mouse_x = e_pos_x;
      this.mouse_y = e_pos_y;
    }
    this.dispatch('mouseMove', {x: e_pos_x,y: e_pos_y });
  }

  dragEnd(e) {
    let nodes = this.container.querySelectorAll(".node_connections_left, .node_connections_right")
    nodes.forEach(n => {
      n.style.visibility = 'visible'; 
    })

    if (e.type === "touchend") {
      var e_pos_x = this.mouse_x;
      var e_pos_y = this.mouse_y;
      var ele_last = document.elementFromPoint(e_pos_x, e_pos_y);
    } else {
      var e_pos_x = e.clientX;
      var e_pos_y = e.clientY;
      var ele_last = e.target;
    }

    if(this.drag) {
      if(this.pos_x_start != e_pos_x || this.pos_y_start != e_pos_y) {
        this.dispatch('nodeMoved', this.ele_selected.id.slice(5));
      }
    }

    if(this.drag_point) {
      this.ele_selected.classList.remove("selected");
        if(this.pos_x_start != e_pos_x || this.pos_y_start != e_pos_y) {
          this.dispatch('rerouteMoved', this.ele_selected.parentElement.classList[2].slice(14));
        }
    }

    if(this.editor_selected) {
      this.canvas_x = this.canvas_x + (-(this.pos_x - e_pos_x));
      this.canvas_y = this.canvas_y + (-(this.pos_y - e_pos_y));
      this.editor_selected = false;
    }
    if(this.connection === true) {
      let node = ele_last.closest('.drawflow-node')

      if(node) {
        var output_id = this.ele_selected.parentElement.parentElement.id;
        var output_class = this.ele_selected.classList[1];
        var input_id = node.id;
        var input_class = output_class === 'node_connection_right' ? 'node_connection_left' : output_class === 'node_connection_left' ? 'node_connection_right' : ''

        if(output_id !== input_id && input_class !== false) {
          if(this.container.querySelectorAll('.connection.node_in_'+input_id+'.node_out_'+output_id+'.'+output_class+'.'+input_class).length === 0) {
          // Conection no exist save connection

          this.connection_ele.classList.add("node_in_"+input_id);
          this.connection_ele.classList.add("node_out_"+output_id);
          this.connection_ele.classList.add(output_class);
          this.connection_ele.classList.add(input_class);

          this.connection_ele.attributes['input_class'] = input_class
          this.connection_ele.attributes['output_class'] = output_class

          var id_input = input_id.slice(5);
          var id_output = output_id.slice(5);

          let type;
          let source = id_output;
          let destination = id_input;
          let id = this.getUuid()

          let sourceName = this.drawflow.drawflow[this.module].data[source].federation.stringId
          let destinationName = this.drawflow.drawflow[this.module].data[destination].federation.stringId
          let connectionText = ''
          if (output_class === 'node_connection_left') {
            type = "input-output"
            connectionText = destinationName + " <- " + sourceName
          }
  
          if (output_class === 'node_connection_right') {
            type = "output-input"
            connectionText = sourceName + " -> " + destinationName
          }

          this.drawflow.drawflow[this.module].data[source].connections.push({type, destination, source, id});

          let textPath = this.connection_ele.querySelectorAll('textPath')[0]
          let path = this.connection_ele.querySelectorAll('path')[0]
          textPath.setAttribute('startOffset', path.getTotalLength() > 250 ? '40%' :  path.getTotalLength() < 150 ? '30%' : '35%');

          textPath.textContent = connectionText
          textPath.setAttribute('sourceName', this.drawflow.drawflow[this.module].data[source].federation.stringId)
          textPath.setAttribute('destinationName', this.drawflow.drawflow[this.module].data[destination].federation.stringId)

          this.updateConnectionNodes('node-'+id_output);
          this.updateConnectionNodes('node-'+id_input);

          this.dispatch('connectionCreated', { 
            output_id: id_output, 
            input_id: id_input, 
            sourceStringId: this.drawflow.drawflow[this.module].data[source].federation.stringId, 
            destinationStringId: this.drawflow.drawflow[this.module].data[destination].federation.stringId,
            output_class:  output_class, 
            input_class: input_class, 
            type, source, destination, id,
          });
        } else {
          this.dispatch('connectionCancel', true);
          this.connection_ele.remove();
        }

          this.connection_ele = null;
      } else {
        // Connection exists Remove Connection;
        this.dispatch('connectionCancel', true);
        this.connection_ele.remove();
        this.connection_ele = null;
      }

      } else {
        // Remove Connection;
        this.dispatch('connectionCancel', true);
        this.connection_ele.remove();
        this.connection_ele = null;
      }
    }

    this.drag = false;
    this.drag_point = false;
    this.connection = false;
    this.ele_selected = null;
    this.editor_selected = false;

    this.dispatch('mouseUp', e);
  }
  contextmenu(e) {
    this.dispatch('contextmenu', e);
    e.preventDefault();
    if(this.editor_mode === 'fixed' || this.editor_mode === 'view') {
      return false;
    }
    if(this.precanvas.getElementsByClassName("drawflow-delete").length) {
      this.precanvas.getElementsByClassName("drawflow-delete")[0].remove()
    };
    if(this.node_selected || this.connection_selected) {
      var deletebox = document.createElement('div');
      deletebox.classList.add("drawflow-delete");
      deletebox.innerHTML = "x";
      if(this.node_selected) {
        this.node_selected.appendChild(deletebox);

      }
      if(this.connection_selected && (this.connection_selected.parentElement.classList.length > 1)) {
        deletebox.style.top = e.clientY * ( this.precanvas.clientHeight / (this.precanvas.clientHeight * this.zoom)) - (this.precanvas.getBoundingClientRect().y *  ( this.precanvas.clientHeight / (this.precanvas.clientHeight * this.zoom)) ) + "px";
        deletebox.style.left = e.clientX * ( this.precanvas.clientWidth / (this.precanvas.clientWidth * this.zoom)) - (this.precanvas.getBoundingClientRect().x *  ( this.precanvas.clientWidth / (this.precanvas.clientWidth * this.zoom)) ) + "px";

        this.precanvas.appendChild(deletebox);

      }

    }

  }
  contextmenuDel() {
    if(this.precanvas.getElementsByClassName("drawflow-delete").length) {
      this.precanvas.getElementsByClassName("drawflow-delete")[0].remove()
    };
  }

  key(e) {
    this.dispatch('keydown', e);
    if(this.editor_mode === 'fixed' || this.editor_mode === 'view') {
      return false;
    }
    if (e.key === 'Delete' || (e.key === 'Backspace' && e.metaKey)) {
      if(this.node_selected != null) {
        if(this.first_click.tagName !== 'INPUT' && this.first_click.tagName !== 'TEXTAREA' && this.first_click.hasAttribute('contenteditable') !== true) {
          this.removeNodeId(this.node_selected.id);
        }
      }
      if(this.connection_selected != null) {
        this.removeConnection();
      }
    }
  }

  zoom_enter(event, delta) {
    if (event.ctrlKey) {
      event.preventDefault()
      if(event.deltaY > 0) {
        // Zoom Out
        this.zoom_out();
      } else {
        // Zoom In
        this.zoom_in();
      }
    }
  }
  zoom_refresh(){
    this.dispatch('zoom', this.zoom);
    this.canvas_x = (this.canvas_x / this.zoom_last_value) * this.zoom;
    this.canvas_y = (this.canvas_y / this.zoom_last_value) * this.zoom;
    this.zoom_last_value = this.zoom;
    this.precanvas.style.transform = "translate("+this.canvas_x+"px, "+this.canvas_y+"px) scale("+this.zoom+")";
  }
  zoom_in(v) {
    const step = (typeof v !== "undefined") ? v : this.zoom_value;
    if(this.zoom < this.zoom_max) {
        this.zoom+=step;
        this.zoom_refresh();
    }
  }
  zoom_out(v) {
    const step = (typeof v !== "undefined") ? v : this.zoom_value;
    if(this.zoom > this.zoom_min) {
      this.zoom-=step;
        this.zoom_refresh();
    }
  }
  zoom_reset(){
    if(this.zoom != 1) {
      this.zoom = 1;
      this.zoom_refresh();
    }
  }

/**
 * Creates an SVG path string for a connection line between two points.
 *
 * The path can be:
 *   - A simple straight line (when `use_arrow` is false)
 *   - A smooth curved connection with directional arrows (when `use_arrow` is true)
 *
 * @param {number} start_pos_x - X coordinate of the line start
 * @param {number} start_pos_y - Y coordinate of the line start
 * @param {number} end_pos_x - X coordinate of the line end
 * @param {number} end_pos_y - Y coordinate of the line end
 * @param {"left"|"right"|"up"|"down"|string} type - Connection direction
 * @param {boolean} use_arrow - Whether to include curve + arrowheads or draw a straight line
 * @returns {string} An SVG path command string
 */
createCurvature(start_pos_x, start_pos_y, end_pos_x, end_pos_y, type, use_arrow) {
  const line_x = start_pos_x;
  const line_y = start_pos_y;
  const x = end_pos_x;
  const y = end_pos_y;

  if (!use_arrow) {
    // SVG path: Move to start (M) → Draw line (L) directly to end point
    return `M ${line_x} ${line_y} L ${x} ${y}`;
  }

  // Horizontal arrow offset (keeps arrow away from node)
  const h_arrow_padding = 15;
  // Vertical arrow offset
  const v_arrow_padding = 9;

  // Distance between nodes (used to proportionally scale curvature)
  const hDiff = Math.abs(line_x - x);
  const vDiff = Math.abs(line_y - y);

  // These scale the "inward pull" of the curve near the endpoints
  const h_end_side_padding = hDiff * 0.4;
  const v_end_side_padding = vDiff * 0.4;

  // Controls how tight the curve bends (smaller = tighter)
  const curveDist = 0.1;

  let curve = "";
  // -----------------------------------------------------
  switch (type) {
    case "right": {
      // Curve centers slightly before the target X position
      const centerCurveX = x - h_end_side_padding - h_arrow_padding;
      const centerCurveY = y;

      // Curve starts bending near the end of the line
      const startCurveX = line_x + (centerCurveX - line_x) * (1 - curveDist);
      const startCurveY = line_y + (centerCurveY - line_y) * (1 - curveDist);

      // Curve ends close to the destination, before arrow padding
      const endCurveX = centerCurveX + (x - centerCurveX) * curveDist;
      const endCurveY = centerCurveY + (y - centerCurveY) * curveDist;

      // SVG path:
      // M = Move to start
      // L = Straight line segment
      // C = Cubic bezier curve (two control points, one endpoint)
      curve =
        `M ${line_x} ${line_y} ` +
        `L ${startCurveX} ${startCurveY} ` +
        `C ${startCurveX} ${startCurveY} ${centerCurveX} ${centerCurveY} ${endCurveX} ${endCurveY} ` +
        `L ${endCurveX} ${endCurveY} ` +
        `L ${x - h_arrow_padding} ${y}`;

      // Add right-pointing arrowheads at the end of the path
      curve +=
        ` M ${x - 11} ${y} L${x - 20} ${y - 5} L${x - 20} ${y + 5} Z` +
        ` M ${x - 11} ${y} L${x - 20} ${y - 3} L${x - 20} ${y + 3} Z` +
        ` M ${x - 11} ${y} L${x - 20} ${y - 1} L${x - 20} ${y + 1} Z`;
      break;
    }
    case "left": {
      const centerCurveX = x + h_end_side_padding + h_arrow_padding;
      const centerCurveY = y;
      const startCurveX = line_x + (centerCurveX - line_x) * (1 - curveDist);
      const startCurveY = line_y + (centerCurveY - line_y) * (1 - curveDist);
      const endCurveX = centerCurveX + (x - centerCurveX) * curveDist;
      const endCurveY = centerCurveY + (y - centerCurveY) * curveDist;

      curve =
        `M ${line_x} ${line_y} ` +
        `L ${startCurveX} ${startCurveY} ` +
        `C ${startCurveX} ${startCurveY} ${centerCurveX} ${centerCurveY} ${endCurveX} ${endCurveY} ` +
        `L ${endCurveX} ${endCurveY} ` +
        `L ${x + h_arrow_padding} ${y}`;

      // Left-pointing arrows
      curve +=
        ` M ${x + 6} ${y} L${x + 15} ${y - 5} L${x + 15} ${y + 5} Z` +
        ` M ${x + 6} ${y} L${x + 15} ${y - 3} L${x + 15} ${y + 3} Z` +
        ` M ${x + 6} ${y} L${x + 15} ${y - 1} L${x + 15} ${y + 1} Z`;
      break;
    }
    case "up": {
      const centerCurveX = x;
      const centerCurveY = y + v_end_side_padding + v_arrow_padding;
      const startCurveX = line_x + (centerCurveX - line_x) * (1 - curveDist);
      const startCurveY = line_y + (centerCurveY - line_y) * (1 - curveDist);
      const endCurveX = centerCurveX + (x - centerCurveX) * curveDist;
      const endCurveY = centerCurveY + (y - centerCurveY) * curveDist;

      curve =
        `M ${line_x} ${line_y} ` +
        `L ${startCurveX} ${startCurveY} ` +
        `C ${startCurveX} ${startCurveY} ${centerCurveX} ${centerCurveY} ${endCurveX} ${endCurveY} ` +
        `L ${endCurveX} ${endCurveY} ` +
        `L ${x} ${y + v_arrow_padding}`;

      // Upward-pointing arrows
      curve +=
        ` M ${x} ${y + 2} L${x - 5} ${y + 11} L${x + 5} ${y + 11} Z` +
        ` M ${x} ${y + 2} L${x - 3} ${y + 11} L${x + 3} ${y + 11} Z` +
        ` M ${x} ${y + 2} L${x - 1} ${y + 11} L${x + 1} ${y + 11} Z`;
      break;
    }
    case "down": {
      const centerCurveX = x;
      const centerCurveY = y - v_end_side_padding - v_arrow_padding;
      const startCurveX = line_x + (centerCurveX - line_x) * (1 - curveDist);
      const startCurveY = line_y + (centerCurveY - line_y) * (1 - curveDist);
      const endCurveX = centerCurveX + (x - centerCurveX) * curveDist;
      const endCurveY = centerCurveY + (y - centerCurveY) * curveDist;

      curve =
        `M ${x} ${y - v_arrow_padding} ` +
        `L ${endCurveX} ${endCurveY} ` +
        `C ${endCurveX} ${endCurveY} ${centerCurveX} ${centerCurveY} ${startCurveX} ${startCurveY} ` +
        `L ${startCurveX} ${startCurveY} ` +
        `L ${line_x} ${line_y}`;

      // Downward-pointing arrows
      curve +=
        ` M ${x} ${y - 2} L${x - 5} ${y - 11} L${x + 5} ${y - 11} Z` +
        ` M ${x} ${y - 2} L${x - 3} ${y - 11} L${x + 3} ${y - 11} Z` +
        ` M ${x} ${y - 2} L${x - 1} ${y - 11} L${x + 1} ${y - 11} Z`;
      break;
    }
    default: {
      // Basic bezier curve (used if direction isn't matched)
      const hx1 = line_x + Math.abs(x - line_x) * 0.5;
      const hx2 = x - Math.abs(x - line_x) * 0.5;
      curve = `M ${line_x} ${line_y} C ${hx1} ${line_y} ${hx2} ${y} ${x} ${y}`;
      break;
    }
  }

  return curve;
}


  drawConnection(ele) {
    let svg = this.createConnectionSVG('','')
    this.connection_ele = svg;

    var id_output = ele.parentElement.parentElement.id.slice(5);
    var output_class = ele.classList[1];

    this.dispatch('connectionStart', { output_id: id_output, output_class:  output_class });
  }
  

  createConnectionSVG(connType, displayText) {
    const uuid = this.getUuid();
    const svgNS = 'http://www.w3.org/2000/svg';

    var connection = document.createElementNS(svgNS, "svg");
    connection.classList.add("connection");

    this.precanvas.appendChild(connection);

    var path = document.createElementNS(svgNS, "path");
    path.classList.add("main-path");
    path.setAttributeNS(null, 'd', '');
    path.setAttributeNS(null, 'id', uuid);
    connection.appendChild(path);
          
    const text = document.createElementNS(svgNS, 'text');
    text.setAttribute('font-size', '14');
    text.setAttribute('fill', 'black');
    text.setAttribute('dy', '-3'); // Move text above the line

    const textPath = document.createElementNS(svgNS, 'textPath');
    textPath.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', ('#' + uuid));
    textPath.setAttribute('startOffset', '40%');
    textPath.setAttribute('text-anchor', 'middle');

    const textContent = document.createTextNode(displayText);
    textPath.appendChild(textContent);
    text.appendChild(textPath);
    connection.appendChild(text);
    
    return connection
  }

  updateConnection(eX, eY) {
    let curveType = this.ele_selected.classList.contains('node_connection_left') ? 'input-output' : 'output-input'

    const precanvas = this.precanvas;
    const zoom = this.zoom;
    let precanvasWidthZoom = precanvas.clientWidth / (precanvas.clientWidth * zoom);
    precanvasWidthZoom = precanvasWidthZoom || 0;
    let precanvasHeightZoom = precanvas.clientHeight / (precanvas.clientHeight * zoom);
    precanvasHeightZoom = precanvasHeightZoom || 0;
    var path = this.connection_ele.children[0];

    var line_x = this.ele_selected.offsetWidth/2 + (this.ele_selected.getBoundingClientRect().x - precanvas.getBoundingClientRect().x ) * precanvasWidthZoom;
    var line_y = this.ele_selected.offsetHeight/2 + (this.ele_selected.getBoundingClientRect().y - precanvas.getBoundingClientRect().y ) * precanvasHeightZoom;

    var x = eX * ( this.precanvas.clientWidth / (this.precanvas.clientWidth * this.zoom)) - (this.precanvas.getBoundingClientRect().x *  ( this.precanvas.clientWidth / (this.precanvas.clientWidth * this.zoom)) );
    var y = eY * ( this.precanvas.clientHeight / (this.precanvas.clientHeight * this.zoom)) - (this.precanvas.getBoundingClientRect().y *  ( this.precanvas.clientHeight / (this.precanvas.clientHeight * this.zoom)) );

    var lineCurve = this.createCurvature(line_x, line_y, x, y, curveType, false);
    path.setAttributeNS(null, 'd', lineCurve);

  }

  addConnection(id_output, id_input, output_class, input_class) {
    var nodeOneModule = this.getModuleFromNodeId(id_output);
    var nodeTwoModule = this.getModuleFromNodeId(id_input);
    if(nodeOneModule === nodeTwoModule) {

      var dataNode = this.getNodeFromId(id_output);
      var exist = false;
      for(var checkOutput in dataNode.connections){
        var connectionSearch = dataNode.connections[checkOutput]
        if(connectionSearch.node == id_input && connectionSearch.output == input_class) {
            exist = true;
        }
      }
      // Check connection exist
      // TODO less hard coding on type and source / dest
      if(exist === false) {
        if (this.ele_selected.classList.contains('node_connection_left')) {
          this.drawflow.drawflow[nodeOneModule].data[id_input].connections.push( {type: "input-output","destination": id_output.toString(), "source": id_input.toString()});
        }

        if (this.ele_selected.classList.contains('node_connection_right')) {
          this.drawflow.drawflow[nodeOneModule].data[id_output].connections.push( {type: "output-input","destination": id_input.toString(), "source": id_output.toString()});
        }

        if(this.module === nodeOneModule) {
        //Draw connection
          var connection = document.createElementNS('http://www.w3.org/2000/svg',"svg");
          var path = document.createElementNS('http://www.w3.org/2000/svg',"path");
          path.classList.add("main-path");
          path.setAttributeNS(null, 'd', '');
          // path.innerHTML = 'a';
          connection.classList.add("connection");
          connection.classList.add("node_in_node-"+id_input);
          connection.classList.add("node_out_node-"+id_output);
          connection.classList.add(output_class);
          connection.classList.add(input_class);
          connection.appendChild(path);
          this.precanvas.appendChild(connection);
          this.updateConnectionNodes('node-'+id_output);
          this.updateConnectionNodes('node-'+id_input);
        }

        this.dispatch('connectionCreated', { output_id: id_output, input_id: id_input, output_class:  output_class, input_class: input_class});
      }
    }
  }

  updateConnectionNodes(id) {
    const idSearch = 'node_in_'+id;
    const idSearchOut = 'node_out_'+id;
    const container = this.container;
    const createCurvature = this.createCurvature;

    const elemsOut = container.querySelectorAll(`.${idSearchOut}`);
    Object.keys(elemsOut).map((item, index) => {
      if(elemsOut[item].querySelector('.point') === null) {
        var id_search = elemsOut[item].classList[1].replace('node_in_', '');

        const sourceNode = container.querySelector(`#${id}`)
        const destNode = container.querySelector(`#${id_search}`)

        let direction = this.calculateEdgeDirection(sourceNode, destNode)
        const lineCurve = createCurvature(direction.startPos.x, direction.startPos.y, direction.endPos.x, direction.endPos.y, direction.lineType, true);
        elemsOut[item].children[0].setAttributeNS(null, 'd', lineCurve );

        this.updateConnectionNodesText(elemsOut[item], direction)
      }
    })

    const elems = container.querySelectorAll(`.${idSearch}`);
    Object.keys(elems).map((item, index) => {
      if(elems[item].querySelector('.point') === null) {
        var id_search = elems[item].classList[2].replace('node_out_', '');

        const sourceNode = container.querySelector(`#${id_search}`)
        const destNode = container.querySelector(`#${id}`)

        let direction = this.calculateEdgeDirection(sourceNode, destNode)
        const lineCurve = createCurvature(direction.startPos.x, direction.startPos.y, direction.endPos.x, direction.endPos.y, direction.lineType, true);
        elems[item].children[0].setAttributeNS(null, 'd', lineCurve );

        this.updateConnectionNodesText(elems[item], direction)
      }
    })
  }

  calculateEdgeDirection(sourceNode, destNode) {
    const precanvasRect = this.precanvas.getBoundingClientRect();
    const zoom = this.zoom;

    const zoomWidthRatio = 1 / zoom;
    const zoomHeightRatio = 1 / zoom;

    const getCenter = (rect, node) => ({
      x: (rect.x - precanvasRect.x) * zoomWidthRatio + node.offsetWidth / 2,
      y: (rect.y - precanvasRect.y) * zoomHeightRatio + node.offsetHeight / 2
    });

    const getIntersectionPoint = (centerA, centerB, node, rect) => {
      const dx = centerB.x - centerA.x;
      const dy = centerB.y - centerA.y;

      const w = node.offsetWidth;
      const h = node.offsetHeight;

      const left = (rect.x - precanvasRect.x) * zoomWidthRatio;
      const top = (rect.y - precanvasRect.y) * zoomHeightRatio;

      const cx = left + w / 2;
      const cy = top + h / 2;

      let px = cx;
      let py = cy;

      const m = dy / dx;

      // Check intersection with vertical sides
      if (Math.abs(dx) > Math.abs(dy)) {
        if (dx > 0) {
          // right
          px = left + w;
          py = cy + m * (px - cx);
        } else {
          // left
          px = left;
          py = cy + m * (px - cx);
        }

        // clamp y to box
        if (py < top || py > top + h) {
          py = Math.max(top, Math.min(top + h, py));
        }
      } else {
        if (dy > 0) {
          // bottom
          py = top + h;
          px = cx + (py - cy) / m;
        } else {
          // top
          py = top;
          px = cx + (py - cy) / m;
        }

        // clamp x to box
        if (px < left || px > left + w) {
          px = Math.max(left, Math.min(left + w, px));
        }
      }

      return { x: px, y: py };
    };

    const sourceRect = sourceNode.getBoundingClientRect();
    const destRect = destNode.getBoundingClientRect();

    const sourceCenter = getCenter(sourceRect, sourceNode);
    const destCenter = getCenter(destRect, destNode);

    const startPos = getIntersectionPoint(sourceCenter, destCenter, sourceNode, sourceRect);
    const endPos = getIntersectionPoint(destCenter, sourceCenter, destNode, destRect);

    // Optional: label line direction based on general vector direction
    const dx = destCenter.x - sourceCenter.x;
    const dy = destCenter.y - sourceCenter.y;
    const angle = Math.atan2(dy, dx) * (180 / Math.PI);

    let lineType = 'right';
    if (angle >= -45 && angle < 45) lineType = 'right';
    else if (angle >= 45 && angle < 135) lineType = 'down';
    else if (angle >= -135 && angle < -45) lineType = 'up';
    else lineType = 'left';

    return { startPos, endPos, lineType };
  }


  updateConnectionNodesText(svg, direction) {
    let text = svg.querySelectorAll('text')[0]
    let textPath = svg.querySelectorAll('textPath')[0]
    let path = svg.querySelectorAll('path')[0]

    const pathLength = path.getTotalLength();

    let hideText = false;

    let rightLineOffset = 45
    let leftLineOffset = 60
    if (pathLength > 400) {
      rightLineOffset = 45
      leftLineOffset = 60
    } else if (pathLength > 250) {
      rightLineOffset = 40
      leftLineOffset = 62.5
    } else if (pathLength > 150) {
      rightLineOffset = 35
      leftLineOffset = 65
    } else {
      hideText = true
    }
    
    let sourceName = '';
    let destinationName = '';
    if (textPath.textContent) {
      if (textPath.textContent.includes('->')) {
        [sourceName, destinationName] = textPath.textContent.split('->');
      } else if (textPath.textContent.includes('<-')) {
        [destinationName, sourceName] = textPath.textContent.split('<-');
      }
    }

    sourceName = sourceName.replace(/\s+/g, '')
    destinationName = destinationName.replace(/\s+/g, '')

    let dy = '-4'
    if (direction.lineType === 'left') {
      textPath.setAttribute('startOffset', (leftLineOffset + '%'));
      textPath.textContent = destinationName + " <- " + sourceName
      const d = path.getAttribute("d");
      path.setAttribute("d", reversePath.reverse(d));
      if (direction.startPos.y > direction.endPos.y) {
        dy = '-4'
      } else {
        dy = '13'
      }
      text.setAttribute('dy', dy); // Move text above the line 
    }

    if (direction.lineType === 'right') {
      textPath.setAttribute('startOffset', (rightLineOffset + '%'));
      textPath.textContent = sourceName + " -> " + destinationName
      if (direction.startPos.y > direction.endPos.y) {
        dy = '-4'
      } else {
        dy = '13'
      }
      text.setAttribute('dy', dy); // Move text above the line 
    }

    if (direction.lineType === 'up') {
      textPath.setAttribute('startOffset', (rightLineOffset + '%'));
      textPath.textContent = sourceName + " -> " + destinationName
      if (direction.startPos.x > direction.endPos.x) {
        dy = '-4'
      } else {
        dy = '13'
      }
      text.setAttribute('dy', dy); // Move text above the line 
    }

    if (direction.lineType === 'down') {
      textPath.setAttribute('startOffset', (rightLineOffset + '%'));
      textPath.textContent = destinationName + " <- " + sourceName
      if (direction.startPos.x > direction.endPos.x) {
        dy = '-4'
      } else {
        dy = '13'
      }
      text.setAttribute('dy', dy); // Move text above the line 
    }   
     text.setAttribute('font-size', hideText ? '0' : '14');
  }

  dblclick(e) {
    if (this.node_selected) {
      this.dispatch('node-dblclick', this.node_selected.id.split('node-')[1]);
    }
    if (this.connection_selected) {
      let source = this.connection_selected.parentElement.classList[2].slice(14);
      let destination = this.connection_selected.parentElement.classList[1].slice(13)

      let foundConnection = this.drawflow.drawflow.Home.data[source]
            .connections.find(c => c.source === source && c.destination === destination);

      this.dispatch('edge-dblclick', foundConnection);
    }
  }

  registerNode(name, html, props = null, options = null) {
    this.noderegister[name] = {html: html, props: props, options: options};
  }

  getNodeFromId(id) {
    var moduleName = this.getModuleFromNodeId(id)
    return JSON.parse(JSON.stringify(this.drawflow.drawflow[moduleName].data[id]));
  }
  getNodesFromName(name) {
    var nodes = [];
    const editor = this.drawflow.drawflow
    Object.keys(editor).map(function(moduleName, index) {
      for (var node in editor[moduleName].data) {
        if(editor[moduleName].data[node].name == name) {
          nodes.push(editor[moduleName].data[node].id);
        }
      }
    });
    return nodes;
  }

  addNode (name, num_in, num_out, ele_pos_x, ele_pos_y, classoverride, data, html, typenode = false) {
    if (this.useuuid) {
      var newNodeId = this.getUuid();
    } else {
      var newNodeId = this.nodeId;
    }
    const parent = document.createElement('div');
    parent.classList.add("parent-node");

    const node = document.createElement('div');
    node.innerHTML = "";
    node.setAttribute("id", "node-"+newNodeId);
    node.classList.add("drawflow-node");
    if(classoverride != '') {
      node.classList.add(...classoverride.split(' '));
    }


    const node_connections_left = document.createElement('div');
    node_connections_left.classList.add("node_connections_left");

    const node_connection_left = document.createElement('div');
    node_connection_left.classList.add("node_connection_point");
    node_connection_left.classList.add("node_connection_left");
    node_connections_left.appendChild(node_connection_left);

    const node_connections_right = document.createElement('div');
    node_connections_right.classList.add("node_connections_right");

    const node_connection_right = document.createElement('div');
    node_connection_right.classList.add("node_connection_point");
    node_connection_right.classList.add("node_connection_right");
    node_connections_right.appendChild(node_connection_right);


    const content = document.createElement('div');
    content.classList.add("drawflow_content_node");
    if(typenode === false) {
      content.innerHTML = html;
    } else if (typenode === true) {
      content.appendChild(this.noderegister[html].html.cloneNode(true));
    }

    Object.entries(data).forEach(function (key, value) {
      if(typeof key[1] === "object") {
        insertObjectkeys(null, key[0], key[0]);
      } else {
        var elems = content.querySelectorAll('[df-'+key[0]+']');
          for(var i = 0; i < elems.length; i++) {
            elems[i].value = key[1];
            if(elems[i].isContentEditable) {
              elems[i].innerText = key[1];
            }
          }
      }
    })

    function insertObjectkeys(object, name, completname) {
      if(object === null) {
        var object = data[name];
      } else {
        var object = object[name]
      }
      if(object !== null) {
        Object.entries(object).forEach(function (key, value) {
          if(typeof key[1] === "object") {
            insertObjectkeys(object, key[0], completname+'-'+key[0]);
          } else {
            var elems = content.querySelectorAll('[df-'+completname+'-'+key[0]+']');
              for(var i = 0; i < elems.length; i++) {
                elems[i].value = key[1];
                if(elems[i].isContentEditable) {
                  elems[i].innerText = key[1];
                }
              }
          }
        });
      }
    }
    if(num_in != 0){
      node.appendChild(node_connections_left);
    }
    node.appendChild(content);
    if(num_out != 0){
      node.appendChild(node_connections_right);
    }
    node.style.top = ele_pos_y + "px";
    node.style.left = ele_pos_x + "px";
    parent.appendChild(node);
    this.precanvas.appendChild(parent);
    var json = {
      id: newNodeId,
      name: name,
      data: data,
      class: classoverride,
      html: html,
      typenode: typenode,
      connections: [],
      pos_x: ele_pos_x,
      pos_y: ele_pos_y,
    }
    this.drawflow.drawflow[this.module].data[newNodeId] = json;
    this.dispatch('nodeCreated', newNodeId);
    if (!this.useuuid) {
      this.nodeId++;
    }
    return newNodeId;
  }

  addNodeImport (dataNode, precanvas) {
    const parent = document.createElement('div');
    parent.classList.add("parent-node");

    const node = document.createElement('div');
    node.innerHTML = "";
    node.setAttribute("id", "node-"+dataNode.id);
    node.classList.add("drawflow-node");
    if(dataNode.class != '') {
      node.classList.add(...dataNode.class.split(' '));
    }

    const inputs = document.createElement('div');
    inputs.classList.add("node_connections_left");

    const input = document.createElement('div');
    input.classList.add("node_connection_point");
    input.classList.add("node_connection_left");
    inputs.appendChild(input);

    const outputs = document.createElement('div');
    outputs.classList.add("node_connections_right");

    const output = document.createElement('div');
    output.classList.add("node_connection_point");
    output.classList.add("node_connection_right");
    outputs.appendChild(output);

    for (let conn of dataNode.connections) {
      let connection = this.createConnectionSVG(conn.type, conn.federation.name)

      connection.classList.add("connection");
      connection.classList.add("node_in_node-"+conn.destination);
      connection.classList.add("node_out_node-"+dataNode.id);

      if (conn.type === 'input-output') {
        connection.classList.add("node_connection_left");
        connection.classList.add("node_connection_right");

        connection.attributes['input_class'] = 'node_connection_right'
        connection.attributes['output_class'] = 'node_connection_left'
      }

      if (conn.type === 'output-input') {
        connection.classList.add("node_connection_right");
        connection.classList.add("node_connection_left");

        connection.attributes['input_class'] = 'node_connection_left'
        connection.attributes['output_class'] = 'node_connection_right'
      }
    }

    const content = document.createElement('div');
    content.classList.add("drawflow_content_node");

    if(dataNode.typenode === false) {
      content.innerHTML = dataNode.html;
    } else if (dataNode.typenode === true) {
      content.appendChild(this.noderegister[dataNode.html].html.cloneNode(true));
    } else {
      if(parseInt(this.render.version) === 3 ) {
        //Vue 3
        let wrapper = this.render.h(this.noderegister[dataNode.html].html, this.noderegister[dataNode.html].props, this.noderegister[dataNode.html].options);
        wrapper.appContext = this.parent;
        this.render.render(wrapper,content);

      } else {
        //Vue 2
        let wrapper = new this.render({
          parent: this.parent,
          render: h => h(this.noderegister[dataNode.html].html, { props: this.noderegister[dataNode.html].props }),
          ...this.noderegister[dataNode.html].options
        }).$mount()
        content.appendChild(wrapper.$el);
      }
    }

    Object.entries(dataNode.data).forEach(function (key, value) {
      if(typeof key[1] === "object") {
        insertObjectkeys(null, key[0], key[0]);
      } else {
        var elems = content.querySelectorAll('[df-'+key[0]+']');
          for(var i = 0; i < elems.length; i++) {
            elems[i].value = key[1];
            if(elems[i].isContentEditable) {
              elems[i].innerText = key[1];
            }
          }
      }
    })

    function insertObjectkeys(object, name, completname) {
      if(object === null) {
        var object = dataNode.data[name];
      } else {
        var object = object[name]
      }
      if(object !== null) {
        Object.entries(object).forEach(function (key, value) {
          if(typeof key[1] === "object") {
            insertObjectkeys(object, key[0], completname+'-'+key[0]);
          } else {
            var elems = content.querySelectorAll('[df-'+completname+'-'+key[0]+']');
              for(var i = 0; i < elems.length; i++) {
                elems[i].value = key[1];
                if(elems[i].isContentEditable) {
                  elems[i].innerText = key[1];
                }
              }
          }
        });
      }
    }
    node.appendChild(inputs);
    node.appendChild(content);
    node.appendChild(outputs);
    node.style.top = dataNode.pos_y + "px";
    node.style.left = dataNode.pos_x + "px";
    parent.appendChild(node);
    this.precanvas.appendChild(parent);
  }

  updateNodeValue(event) {
    var attr = event.target.attributes
    for (var i = 0; i < attr.length; i++) {
            if (attr[i].nodeName.startsWith('df-')) {
                var keys = attr[i].nodeName.slice(3).split("-");
                var target = this.drawflow.drawflow[this.module].data[event.target.closest(".drawflow_content_node").parentElement.id.slice(5)].data;
                for (var index = 0; index < keys.length - 1; index += 1) {
                    if (target[keys[index]] == null) {
                        target[keys[index]] = {};
                    }
                    target = target[keys[index]];
                }
                target[keys[keys.length - 1]] = event.target.value;
                if(event.target.isContentEditable) {
                  target[keys[keys.length - 1]] = event.target.innerText;
                }
                this.dispatch('nodeDataChanged', event.target.closest(".drawflow_content_node").parentElement.id.slice(5));
          }
    }
  }

  updateNodeDataFromId(id, data) {
    var moduleName = this.getModuleFromNodeId(id)
    this.drawflow.drawflow[moduleName].data[id].data = data;
    if(this.module === moduleName) {
      const content = this.container.querySelector('#node-'+id);

      Object.entries(data).forEach(function (key, value) {
        if(typeof key[1] === "object") {
          insertObjectkeys(null, key[0], key[0]);
        } else {
          var elems = content.querySelectorAll('[df-'+key[0]+']');
            for(var i = 0; i < elems.length; i++) {
              elems[i].value = key[1];
              if(elems[i].isContentEditable) {
                elems[i].innerText = key[1];
              }
            }
        }
      })

      function insertObjectkeys(object, name, completname) {
        if(object === null) {
          var object = data[name];
        } else {
          var object = object[name]
        }
        if(object !== null) {
          Object.entries(object).forEach(function (key, value) {
            if(typeof key[1] === "object") {
              insertObjectkeys(object, key[0], completname+'-'+key[0]);
            } else {
              var elems = content.querySelectorAll('[df-'+completname+'-'+key[0]+']');
                for(var i = 0; i < elems.length; i++) {
                  elems[i].value = key[1];
                  if(elems[i].isContentEditable) {
                    elems[i].innerText = key[1];
                  }
                }
            }
          });
        }
      }

    }
  }

  addNodeInput(id) {
    var moduleName = this.getModuleFromNodeId(id)
    const infoNode = this.getNodeFromId(id)
    const numInputs = Object.keys(infoNode.inputs).length;
    if(this.module === moduleName) {
      //Draw input
      const input = document.createElement('div');
      input.classList.add("input");
      input.classList.add("input_"+(numInputs+1));
      const parent = this.container.querySelector('#node-'+id+' .inputs');
      parent.appendChild(input);
      this.updateConnectionNodes('node-'+id);

    }
  }

  addNodeOutput(id) {
    var moduleName = this.getModuleFromNodeId(id)
    const infoNode = this.getNodeFromId(id)
    const numOutputs = Object.keys(infoNode.outputs).length;
    if(this.module === moduleName) {
      //Draw output
      const output = document.createElement('div');
      output.classList.add("output");
      output.classList.add("output_"+(numOutputs+1));
      const parent = this.container.querySelector('#node-'+id+' .outputs');
      parent.appendChild(output);
      this.updateConnectionNodes('node-'+id);

    }
  }

  removeNodeInput(id, input_class) {
    var moduleName = this.getModuleFromNodeId(id)
    const infoNode = this.getNodeFromId(id)
    if(this.module === moduleName) {
      this.container.querySelector('#node-'+id+' .inputs .input.'+input_class).remove();
    }
    const removeInputs = [];
    Object.keys(infoNode.connections).map(function(key, index) {
      const id_output = infoNode.connections[index].node;
      const output_class = infoNode.connections[index].input;
      removeInputs.push({id_output, id, output_class, input_class})
    })

    // Remove connections
    removeInputs.forEach((item, i) => {
      this.removeSingleConnection(item.id_output, item.id, item.output_class, item.input_class);
    });

    delete this.drawflow.drawflow[moduleName].data[id];

    // Update connection
    const connections = [];
    const connectionsInputs = this.drawflow.drawflow[moduleName].data[id].inputs
    Object.keys(connectionsInputs).map(function(key, index) {
      connections.push(connectionsInputs[key]);
    });
    this.drawflow.drawflow[moduleName].data[id].inputs = {};
    const input_class_id = input_class.slice(6);
    let nodeUpdates = [];
    connections.forEach((item, i) => {
      item.connections.forEach((itemx, f) => {
        nodeUpdates.push(itemx);
      });
      this.drawflow.drawflow[moduleName].data[id].inputs['input_'+ (i+1)] = item;
    });
    nodeUpdates =  new Set(nodeUpdates.map(e => JSON.stringify(e)));
    nodeUpdates = Array.from(nodeUpdates).map(e => JSON.parse(e));

    if(this.module === moduleName) {
      const eles = this.container.querySelectorAll("#node-"+id +" .inputs .input");
      eles.forEach((item, i) => {
        const id_class = item.classList[1].slice(6);
        if(parseInt(input_class_id) < parseInt(id_class)) {
          item.classList.remove('input_'+id_class);
          item.classList.add('input_'+(id_class-1));
        }
      });

    }

    nodeUpdates.forEach((itemx, i) => {
      this.drawflow.drawflow[moduleName].data[itemx.node].connections.forEach((itemz, g) => {
          if(itemz.node == id) {
            const output_id = itemz.output.slice(6);
            if(parseInt(input_class_id) < parseInt(output_id)) {
              if(this.module === moduleName) {
                const ele = this.container.querySelector(".connection.node_in_node-"+id+".node_out_node-"+itemx.node+"."+itemx.input+".input_"+output_id);
                ele.classList.remove('input_'+output_id);
                ele.classList.add('input_'+(output_id-1));
              }
              if(itemz.points) {
                  this.drawflow.drawflow[moduleName].data[itemx.node].connections[g] = { node: itemz.node, output: 'input_'+(output_id-1), points: itemz.points }
              } else {
                  this.drawflow.drawflow[moduleName].data[itemx.node].connections[g] = { node: itemz.node, output: 'input_'+(output_id-1)}
              }
            }
          }
      });
    });
    this.updateConnectionNodes('node-'+id);
  }

  removeNodeOutput(id, output_class) {
    var moduleName = this.getModuleFromNodeId(id)
    const infoNode = this.getNodeFromId(id)
    if(this.module === moduleName) {
      this.container.querySelector('#node-'+id+' .outputs .output.'+output_class).remove();
    }
    const removeOutputs = [];
    Object.keys(infoNode.connections).map(function(key, index) {
      const id_input = infoNode.connections[index].node;
      const input_class = infoNode.connections[index].output;
      removeOutputs.push({id, id_input, output_class, input_class})
    })

    // Remove connections
    removeOutputs.forEach((item, i) => {
      this.removeSingleConnection(item.id, item.id_input, item.output_class, item.input_class);
    });

    delete this.drawflow.drawflow[moduleName].data[id];

    // Update connection
    const connections = [];
    const connectionsOuputs = this.drawflow.drawflow[moduleName].data[id].outputs
    Object.keys(connectionsOuputs).map(function(key, index) {
      connections.push(connectionsOuputs[key]);
    });
    this.drawflow.drawflow[moduleName].data[id].outputs = {};
    const output_class_id = output_class.slice(7);
    let nodeUpdates = [];
    connections.forEach((item, i) => {
      item.connections.forEach((itemx, f) => {
        nodeUpdates.push({ node: itemx.node, output: itemx.output });
      });
      this.drawflow.drawflow[moduleName].data[id].outputs['output_'+ (i+1)] = item;
    });
    nodeUpdates =  new Set(nodeUpdates.map(e => JSON.stringify(e)));
    nodeUpdates = Array.from(nodeUpdates).map(e => JSON.parse(e));

    if(this.module === moduleName) {
      const eles = this.container.querySelectorAll("#node-"+id +" .outputs .output");
      eles.forEach((item, i) => {
        const id_class = item.classList[1].slice(7);
        if(parseInt(output_class_id) < parseInt(id_class)) {
          item.classList.remove('output_'+id_class);
          item.classList.add('output_'+(id_class-1));
        }
      });

    }

    nodeUpdates.forEach((itemx, i) => {
      this.drawflow.drawflow[moduleName].data[itemx.node].connections.forEach((itemz, g) => {
          if(itemz.node == id) {
            const input_id = itemz.input.slice(7);
            if(parseInt(output_class_id) < parseInt(input_id)) {
              if(this.module === moduleName) {

                const ele = this.container.querySelector(".connection.node_in_node-"+itemx.node+".node_out_node-"+id+".output_"+input_id+"."+itemx.output);
                ele.classList.remove('output_'+input_id);
                ele.classList.remove(itemx.output);
                ele.classList.add('output_'+(input_id-1));
                ele.classList.add(itemx.output);
              }
              if(itemz.points) {
                  this.drawflow.drawflow[moduleName].data[itemx.node].connections[g] = { node: itemz.node, input: 'output_'+(input_id-1), points: itemz.points }
              } else {
                  this.drawflow.drawflow[moduleName].data[itemx.node].connections[g] = { node: itemz.node, input: 'output_'+(input_id-1)}
              }
            }
          }
      });
    });

    this.updateConnectionNodes('node-'+id);
  }

  removeNodeId(id) {
    this.removeConnectionsToNode(id);

    var moduleName = this.getModuleFromNodeId(id.slice(5))
    if(this.module === moduleName) {
      this.container.querySelector(`#${id}`).remove();
    }
    delete this.drawflow.drawflow[moduleName].data[id.slice(5)];
    this.dispatch('nodeRemoved', id.slice(5));
  }

  removeConnection() {
    if(this.connection_selected != null) {
      var listclass = this.connection_selected.parentElement.classList;
      this.connection_selected.parentElement.remove();

      var index_out = this.drawflow.drawflow[this.module].data[listclass[2].slice(14)].connections.findIndex(function(item,i) {
        return item.destination === listclass[1].slice(13)
      });

      this.drawflow.drawflow[this.module].data[listclass[2].slice(14)].connections.splice(index_out,1);

      this.dispatch('connectionRemoved', { output_id: listclass[2].slice(14), input_id: listclass[1].slice(13), output_class: listclass[3], input_class: listclass[4] } );
      this.connection_selected = null;
    }
  }

  removeSingleConnection(id_output, id_input, output_class, input_class) {
    var nodeOneModule = this.getModuleFromNodeId(id_output);
    var nodeTwoModule = this.getModuleFromNodeId(id_input);

    if(nodeOneModule === nodeTwoModule) {
      // Check nodes in same module.

        if(this.module === nodeOneModule) {
          // In same module with view.
          this.container.querySelector('.connection.node_in_node-'+id_input+'.node_out_node-'+id_output+'.'+output_class+'.'+input_class).remove();
        }

        var index_out = this.drawflow.drawflow[nodeOneModule].data[id_output].connections.findIndex(function(item,i) {
          return item.node == id_input && item.output === input_class
        });
        this.drawflow.drawflow[nodeOneModule].data[id_output].connections.splice(index_out,1);

        var index_in = this.drawflow.drawflow[nodeOneModule].data[id_input].connections.findIndex(function(item,i) {
          return item.node == id_output && item.input === output_class
        });
        this.drawflow.drawflow[nodeOneModule].data[id_input].connections.splice(index_in,1);

        this.dispatch('connectionRemoved', { output_id: id_output, input_id: id_input, output_class:  output_class, input_class: input_class});
        return true;
    } else {
      return false;
    }
  }

  removeConnectionsToNode(nodeId) {
    let connections = this.container.querySelectorAll('.node_in_' + nodeId + ', .node_out_' + nodeId)
    connections.forEach(connection => {
      var listclass = connection.classList;
      connection.remove();
      
      let connectionOriginId = listclass[2].slice(14)

      var index = this.drawflow.drawflow[this.module].data[connectionOriginId].connections.findIndex(function(item,i) {
        return item.destination === listclass[1].slice(13)
      });

      if (index > -1) {
        this.drawflow.drawflow[this.module].data[connectionOriginId].connections.splice(index,1);
      }

      this.dispatch('connectionRemoved', { output_id: connectionOriginId, input_id: listclass[1].slice(13), output_class: listclass[3], input_class: listclass[4] } );
    })
  }

  getModuleFromNodeId(id) {
    var nameModule;
    const editor = this.drawflow.drawflow
    Object.keys(editor).map(function(moduleName, index) {
      Object.keys(editor[moduleName].data).map(function(node, index2) {
        if(node == id) {
          nameModule = moduleName;
        }
      })
    });
    return nameModule;
  }

  addModule(name) {
    this.drawflow.drawflow[name] =  { "data": {} };
    this.dispatch('moduleCreated', name);
  }
  changeModule(name) {
    this.dispatch('moduleChanged', name);
    this.module = name;
    this.precanvas.innerHTML = "";
    this.canvas_x = 0;
    this.canvas_y = 0;
    this.pos_x = 0;
    this.pos_y = 0;
    this.mouse_x = 0;
    this.mouse_y = 0;
    this.zoom = 1;
    this.zoom_last_value = 1;
    this.precanvas.style.transform = '';
    this.import(this.drawflow, false);
  }

  removeModule(name) {
    if(this.module === name) {
      this.changeModule('Home');
    }
    delete this.drawflow.drawflow[name];
    this.dispatch('moduleRemoved', name);
  }

  clearModuleSelected() {
    this.precanvas.innerHTML = "";
    this.drawflow.drawflow[this.module] =  { "data": {} };
  }

  clear () {
    this.precanvas.innerHTML = "";
    this.drawflow = { "drawflow": { "Home": { "data": {} }}};
  }
  exportCells () {
    let cells = []
    for (let node of Object.values(this.drawflow.drawflow.Home.data)) {
      let nodeCopy = JSON.parse(JSON.stringify(node))
      nodeCopy.graphType = node.name;
      nodeCopy.roger_federation = nodeCopy.federation
      cells.push(nodeCopy);

      for (let conn of nodeCopy.connections) {
        let connCopy = JSON.parse(JSON.stringify(conn))
        connCopy.graphType = 'EdgeCell';
        connCopy.source = {id: conn.source}
        connCopy.target = {id: conn.destination}
        connCopy.roger_federation = connCopy.federation;
        cells.push(connCopy);
      }
    }

    this.dispatch('export', cells);
    return cells;
  }

  exportSettings () {
    return {
      zoom: this.zoom,
      canvas_x: this.canvas_x,
      canvas_y: this.canvas_y
    };
  }

  applySettings(settings) {
    if (settings) {
      this.zoom = settings.zoom
      this.canvas_x = settings.canvas_x
      this.canvas_y = settings.canvas_y

      this.dispatch('translate', { x: this.canvas_x , y: this.canvas_y });
      this.precanvas.style.transform = "translate("+this.canvas_x+"px, "+this.canvas_y+"px) scale("+this.zoom+")"
    }
  } 

  import (data, notifi = true) {
    this.clear();
    this.drawflow = JSON.parse(JSON.stringify(data));
    this.load();
    if(notifi) {
      this.dispatch('import', 'import');
    }
  }

  /* Events */
  on (event, callback) {
       // Check if the callback is not a function
       if (typeof callback !== 'function') {
           console.error(`The listener callback must be a function, the given type is ${typeof callback}`);
           return false;
       }
       // Check if the event is not a string
       if (typeof event !== 'string') {
           console.error(`The event name must be a string, the given type is ${typeof event}`);
           return false;
       }
       // Check if this event not exists
       if (this.events[event] === undefined) {
           this.events[event] = {
               listeners: []
           }
       }
       this.events[event].listeners.push(callback);
   }

   removeListener (event, callback) {
      // Check if this event not exists

      if (!this.events[event]) return false

      const listeners = this.events[event].listeners
      const listenerIndex = listeners.indexOf(callback)
      const hasListener = listenerIndex > -1
      if (hasListener) listeners.splice(listenerIndex, 1)
   }

   dispatch (event, details) {
       // Check if this event not exists
       if (this.events[event] === undefined) {
           // console.error(`This event: ${event} does not exist`);
           return false;
       }
       this.events[event].listeners.forEach((listener) => {
           listener(details);
       });
   }

    getUuid() {
        // http://www.ietf.org/rfc/rfc4122.txt
        var s = [];
        var hexDigits = "0123456789abcdef";
        for (var i = 0; i < 36; i++) {
            s[i] = hexDigits.substr(Math.floor(Math.random() * 0x10), 1);
        }
        s[14] = "4";  // bits 12-15 of the time_hi_and_version field to 0010
        s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);  // bits 6-7 of the clock_seq_hi_and_reserved to 01
        s[8] = s[13] = s[18] = s[23] = "-";

        var uuid = s.join("");
        return uuid;
    }
}