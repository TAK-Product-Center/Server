import $ from 'jquery';

import { ToolbarComponent } from './toolbar/toolbar.component';
// modal
import { AddCAGroupModalComponent } from './modal/add-ca-group-modal/add-ca-group-modal.component';
import { AddOutgoingConnectionModalComponent } from './modal/add-outgoing-connection-modal/add-outgoing-connection-modal.component';
import { AddEdgeModalComponent } from './modal/add-edge-modal/add-edge-modal.component';
import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  ViewChild,
  ViewContainerRef,
  ChangeDetectorRef,
} from '@angular/core';
import Drawflowz from '../../assets/drawflow';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import { MatDialog } from '@angular/material/dialog';
import { AddTokenGroupModalComponent } from './modal/add-token-group-modal/add-token-group-modal.component';

import { DataService } from '../data.service';
import { WorkflowService } from '../workflow.service';

import { CaGroupComponent } from './node/ca-group/ca-group.component';
import { TokenGroupComponent } from './node/token-group/token-group.component';
import { OutgoingConnectionComponent } from './node/outgoing-connection/outgoing-connection.component';

import { HttpClient } from '@angular/common/http';
import { Annotation } from './node/annotation/annotation';
import { AddAnnotationModal } from './modal/add-annotation-modal/add-annotation-modal';
@Component({
  selector: 'app-drawflow',
  imports: [
    CommonModule,
    ToolbarComponent,
    AddCAGroupModalComponent,
    AddOutgoingConnectionModalComponent,
    AddEdgeModalComponent,
    CaGroupComponent,
    TokenGroupComponent,
    OutgoingConnectionComponent,
  ],
  templateUrl: './drawflow.component.html',
  styleUrl: './drawflow.component.css',
})
export class DrawflowComponent implements OnInit {
  readonly dialog = inject(MatDialog);

  graphNodes: any[] = [
    {
      id: 'GroupCell',
      title: 'CA Group',
      image:
        'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA1MTIgNTEyIj48IS0tIUZvbnQgQXdlc29tZSBGcmVlIDYuNy4yIGJ5IEBmb250YXdlc29tZSAtIGh0dHBzOi8vZm9udGF3ZXNvbWUuY29tIExpY2Vuc2UgLSBodHRwczovL2ZvbnRhd2Vzb21lLmNvbS9saWNlbnNlL2ZyZWUgQ29weXJpZ2h0IDIwMjUgRm9udGljb25zLCBJbmMuLS0+PHBhdGggZD0iTTQ2NCAyNTZBMjA4IDIwOCAwIDEgMCA0OCAyNTZhMjA4IDIwOCAwIDEgMCA0MTYgMHpNMCAyNTZhMjU2IDI1NiAwIDEgMSA1MTIgMEEyNTYgMjU2IDAgMSAxIDAgMjU2eiIvPjwvc3ZnPg==',
      image_height: '25px',
      image_width: '25px',
      marginRight: '5',
      marginLeft: '5',
    },
    // {
    //   id: 'federation',
    //   title: 'Federation',
    //   image: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAWAgMAAACnE7QbAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAYdEVYdFNvZnR3YXJlAHBhaW50Lm5ldCA0LjAuOWwzfk4AAAAJUExURQAAAP///wAAAHPGg3EAAAACdFJOUwAAdpPNOAAAACdJREFUCNdjYGBatYqBgYkBDIinQCwHOI9xFapcA4iNkLvWtIJ0GwDTEwWTtBTeUwAAAABJRU5ErkJggg==",
    //   image_height: '20px',
    //   image_width: '25px',
    //   marginRight: '5',
    //   marginLeft: '5'
    // },
    {
      id: 'FederationOutgoingCell',
      title: 'Outgoing',
      image:
        'data:image/png;base64,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',
      image_height: '25px',
      image_width: '25px',
      marginRight: '5',
      marginLeft: '5',
    },
    {
      id: 'FederationTokenGroupCell',
      title: 'Token Group',
      image:
        'data:image/png;base64,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',
      image_height: '25px',
      image_width: '35px',
      marginRight: '0',
      marginLeft: '0',
    },
    {
      id: 'PolicyTextAnnotation',
      title: 'Text Annotation',
      image:
        'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA1MTIgNTEyIj48IS0tIUZvbnQgQXdlc29tZSBGcmVlIDYuNy4yIGJ5IEBmb250YXdlc29tZSAtIGh0dHBzOi8vZm9udGF3ZXNvbWUuY29tIExpY2Vuc2UgLSBodHRwczovL2ZvbnRhd2Vzb21lLmNvbS9saWNlbnNlL2ZyZWUgQ29weXJpZ2h0IDIwMjUgRm9udGljb25zLCBJbmMuLS0+PHBhdGggZD0iTTY0IDBDMjguNyAwIDAgMjguNyAwIDY0TDAgMzUyYzAgMzUuMyAyOC43IDY0IDY0IDY0bDk2IDAgMCA4MGMwIDYuMSAzLjQgMTEuNiA4LjggMTQuM3MxMS45IDIuMSAxNi44LTEuNUwzMDkuMyA0MTYgNDQ4IDQxNmMzNS4zIDAgNjQtMjguNyA2NC02NGwwLTI4OGMwLTM1LjMtMjguNy02NC02NC02NEw2NCAweiIvPjwvc3ZnPg==',
      image_height: '25px',
      image_width: '35px',
      marginRight: '0',
      marginLeft: '0',
    },
  ];

  nodes: any[] = [];
  locked: boolean = false;
  showLock: boolean = false;
  showNodes: boolean = false;
  otherDetails: any;

  editor!: any;
  editDivHtml: HTMLElement | undefined;
  editButtonShown: boolean = false;

  drawnNodes: any[] = [];
  selectedNodeId: string = '';
  selectedNode: any = {};

  lastMousePositionEv: any;

  mobile_item_selec: string = '';
  mobile_last_move: TouchEvent | undefined;

  private pollingTimeoutId: any;
  private updateInterval: any;
  private updateIntervalMs = 5000; // update interval in milliseconds
  private useDummyData = false;
  private metricsApiUrl = '/fig/getBrokerMetrics';
  private UUIDDecoder = new Map<string, string>();
  private nodeStatsDictionary = new Map<string, number>();
  private testStatsDictionary = new Map<string, number>();

  constructor(private cdr: ChangeDetectorRef, private dataService: DataService, private router: Router,
    private workflowService: WorkflowService, private route: ActivatedRoute, private http: HttpClient) { }

  ngOnInit() {
    // Fetch metrics immediately and then every 5 seconds
    this.fetchMetrics();
    this.updateInterval = setInterval(() => this.fetchMetrics(), this.updateIntervalMs);
  }

  ngOnDestroy() {
    if (this.pollingTimeoutId) {
      clearTimeout(this.pollingTimeoutId);
    }
  }

  getRandomNumber(min: number, max: number): number {
    return Math.random() * (max - min) + min;
  }

  fetchMetrics() {
    if (this.useDummyData) {
      var nodes = this.nodes;

      for (var node of nodes) {
        var id = node.id;
        var nodeClassIn = `node_in_node-${id}`;
        var randomIn = Math.round(this.getRandomNumber(0, 1));
        var testValue = (this.testStatsDictionary.get(id) ?? 0) + randomIn;
        this.testStatsDictionary.set(id, testValue);
        var oldStat = this.nodeStatsDictionary.get(id) ?? 0;
        var t = document.querySelector(`.${nodeClassIn}`);
        if (t) {
          var connection = t.querySelector('.main-path');
          if (connection) {
            if (testValue > oldStat) {
              connection.classList.add("data-flowing");
              oldStat = testValue;
              this.nodeStatsDictionary.set(id, oldStat);
            }
            else {
              connection.classList.remove("data-flowing");
            }
          }
        }
      }
      console.log('Using dummy data:');

    } else {
      this.http.get<any>(this.metricsApiUrl).subscribe(
        (data) => {          
          for(var channel of data.channelInfos){
            var channelKey = `${channel.sourceCert}|${channel.targetCert}`;
            var lastStat = (this.nodeStatsDictionary.get(channelKey) ?? 0);
            var decodedUUID = this.UUIDDecoder.get(channel.sourceCert);
            if(decodedUUID){
              var nodeClassIn = `node_out_node-${decodedUUID}`;
              var t = document.querySelector(`.${nodeClassIn}`);
              if (t) {
                var connection = t.querySelector('.main-path');
                if (connection) {
                  if(channel.messagesWritten > lastStat){
                    connection.classList.add("data-flowing");
                    this.nodeStatsDictionary.set(channelKey, channel.messagesWritten);
                  }
                  else {
                    connection.classList.remove("data-flowing");
                  }
                }
              }
            }
          }
        },
        (error) => console.error('Error fetching metrics:', error)
      );
    }
    this.cdr.detectChanges();
  }

  @ViewChild('dynamic_loader', { read: ViewContainerRef, static: true }) customPlaceholder!: ViewContainerRef;

  ngAfterViewInit(): void {
    this.initDrawingBoard();
    this.pollActiveConnections();
  }

  private initDrawingBoard() {
    this.initDrawFlow();
    if (!this.locked) {
      this.addEditorEvents();
      this.dragEvent();
    }
  }

  private getCentroid(coords: any) {
    // Step 1: Calculate the centroid (average of all x and y coordinates)
    let sumX = -1000;
    let sumY = -1000;
    let n = coords.length;

    for (let i = 0; i < n; i++) {
      sumX += coords[i][0];
      sumY += coords[i][1];
    }

    let centroidX = sumX / n;
    let centroidY = sumY / n;

    return [centroidX, centroidY];
  }

  // Private functions
  private initDrawFlow(): void {
    if (typeof document !== 'undefined') {
      const drawFlowHtmlElement = document.getElementById('drawflow');

      this.editor = new Drawflowz(drawFlowHtmlElement as HTMLElement);

      this.editor.reroute = true;
      this.editor.curvature = 0.5;
      this.editor.reroute_fix_curvature = true;
      this.editor.reroute_curvature = 0.5;
      this.editor.force_first_input = false;
      this.editor.line_path = 1;
      this.editor.editor_mode = 'edit';

      this.dataService.setEditor(this.editor);
      this.editor.start();

      this.route.queryParamMap.subscribe((params) => {
        const policyId = params.get('policy');
        if (!policyId) {
          this.router.navigate(['/home']);
          return;
        }

        this.workflowService.loadGraph(policyId).subscribe({
          next: (res) => {
            if (!res || !res.name) {
              console.log('No polcy found for this name!');
              this.router.navigate(['/home']);
              return;
            }

            let graph: any = {
              drawflow: { Home: { data: {} } },
            };

            console.log('Fetch Graph: ', res)

            let version = res.version;

            if (version?.toLowerCase() === 'v2') {
              console.log('V2 version found in policy.')
              if (res.cells) {
                for (let cell of res.cells) {
                  if (
                    cell.graphType === 'GroupCell' ||
                    cell.graphType === 'FederationTokenGroupCell' ||
                    cell.graphType === 'FederationOutgoingCell'
                  ) {
                    graph['drawflow']['Home']['data'][cell.id] = cell;
                  }
                }
                console.log('Importing graph for rendering:', graph)
                this.editor.import(graph);
                this.editor.applySettings(res.settings)
              }
            }
            // if old version or no version found, we need to convert the old format to the new one
            else {
              console.log('No version found. Attempting to upgrade policy to current format.')
              if (res.cells) {
                // lets try to make sure the graph starts at 0,0 by translating it
                let originalCoords = [];
                for (let cell of res.cells) {
                  if (
                    cell.graphType === 'GroupCell' ||
                    cell.graphType === 'FederationTokenGroupCell' ||
                    cell.graphType === 'FederationOutgoingCell'
                  ) {
                    originalCoords.push([cell.position.x, cell.position.y]);
                  }
                }
                let centroid = this.getCentroid(originalCoords);

                // for each graph node, convert the old format to the new one
                for (let cell of res.cells) {
                  if (
                    cell.graphType === 'GroupCell' ||
                    cell.graphType === 'FederationTokenGroupCell' ||
                    cell.graphType === 'FederationOutgoingCell'
                  ) {
                    let v1ToV2Cell = {
                      id: cell.id,
                      name: cell.graphType,
                      data: {},
                      class: cell.graphType,
                      typenode: false,
                      connections: [],
                      html: '',
                      pos_x: cell.position.x - centroid[0],
                      pos_y: cell.position.y - centroid[1],
                      federation: cell.roger_federation,
                    };
                    v1ToV2Cell.federation.node_id = cell.id;

                    switch (cell.graphType) {
                      case 'PolicyTextAnnotation': {
                        var nodeProperties = this.graphNodes.filter(
                          (n) => n.id === 'PolicyTextAnnotation'
                        )[0];
                        var PolicyTextAnnotation = this.renderComponent(
                          Annotation, // Use your new component
                          v1ToV2Cell.federation, // Pass relevant data (e.g., text)
                          nodeProperties
                        );
                        v1ToV2Cell.html = PolicyTextAnnotation;
                        break;
                      }
                      case 'GroupCell': {
                        var nodeProperties = this.graphNodes.filter(
                          (n) => n.id === 'GroupCell'
                        )[0];
                        var GroupCell = this.renderComponent(
                          CaGroupComponent,
                          v1ToV2Cell.federation,
                          nodeProperties
                        );
                        v1ToV2Cell.html = GroupCell;
                        break;
                      }
                      case 'FederationTokenGroupCell': {
                        var nodeProperties = this.graphNodes.filter(
                          (n) => n.id === 'FederationTokenGroupCell'
                        )[0];
                        var FederationTokenGroupCell = this.renderComponent(
                          TokenGroupComponent,
                          v1ToV2Cell.federation,
                          nodeProperties
                        );
                        v1ToV2Cell.html = FederationTokenGroupCell;
                        break;
                      }
                      case 'FederationOutgoingCell': {
                        var nodeProperties = this.graphNodes.filter(
                          (n) => n.id === 'FederationOutgoingCell'
                        )[0];
                        var FederationOutgoingCell = this.renderComponent(
                          OutgoingConnectionComponent,
                          v1ToV2Cell.federation,
                          nodeProperties
                        );
                        v1ToV2Cell.html = FederationOutgoingCell;
                        break;
                      }
                      default: {
                        break;
                      }
                    }

                    graph['drawflow']['Home']['data'][cell.id] = v1ToV2Cell;
                  }
                }
                // for each graph edge, convert the old format to the new one
                for (let cell of res.cells) {
                  if (cell.graphType === 'EdgeCell') {
                    let sourceX =
                      graph['drawflow']['Home']['data'][cell.source.id].pos_x;
                    let targetX =
                      graph['drawflow']['Home']['data'][cell.target.id].pos_y;

                    let type;
                    if (sourceX > targetX) {
                      type = 'input-output';
                    } else {
                      type = 'output-input';
                    }

                    let connection = {
                      id: cell.id,
                      type,
                      destination: cell.target.id,
                      source: cell.source.id,
                      federation: cell.roger_federation,
                    };

                    graph['drawflow']['Home']['data'][
                      cell.source.id
                    ].connections.push(connection);
                  }
                }
                console.log('Importing updated graph for rendering:', graph)
                this.editor.import(graph);
              }
            }

            this.dataService.setActivePolicy(res);

            setTimeout(() => {
              for (var key in this.editor.drawflow.drawflow.Home.data) {
                this.editor.updateConnectionNodes('node-'+key);
              }
            }, 100)
          },
          error: (e) => console.log(e),
        });
      });
    }
  }

  private addEditorEvents() {
    // Events!

    this.editor.on('node-dblclick', (id: any) => {
      let editNode = this.editor.drawflow.drawflow.Home.data[`${id}`];
      if (editNode) {
        this.openModalForNode(editNode);
      }
    });

    this.editor.on('edge-dblclick', (connection: any) => {
      if (connection) {
        this.openModalForEdge(connection);
      }
    });

    this.editor.on('nodeCreated', (id: any) => {
      let createdNode = this.editor.getNodeFromId(id);

      if (createdNode) {
        this.openModalForNode(createdNode);
      }
    });

    this.editor.on('nodeRemoved', (id: any) => {
      console.log('Editor Event :>> Node removed ' + id);
    });

    this.editor.on('nodeSelected', (id: any) => {
      console.log(
        'Editor Event :>> Node selected ' + id,
        this.editor.getNodeFromId(id)
      );
      this.selectedNode = this.editor.drawflow.drawflow.Home.data[`${id}`];
      console.log(
        'Editor Event :>> Node selected :>> this.selectedNode :>> ',
        this.selectedNode
      );
      console.log(
        'Editor Event :>> Node selected :>> this.selectedNode :>> ',
        this.selectedNode.data
      );
    });

    this.editor.on('click', (e: any) => {
      console.log('Editor Event :>> Click :>> ', e);

      if (
        e.target.closest('.drawflow_content_node') != null ||
        e.target.classList[0] === 'drawflow-node'
      ) {
        if (e.target.closest('.drawflow_content_node') != null) {
          this.selectedNodeId = e.target.closest(
            '.drawflow_content_node'
          ).parentElement.id;
        } else {
          this.selectedNodeId = e.target.id;
        }
        this.selectedNode =
          this.editor.drawflow.drawflow.Home.data[
            `${this.selectedNodeId.slice(5)}`
          ];
      }

      if (
        e.target.closest('#editNode') != null ||
        e.target.classList[0] === 'edit-node-button'
      ) {
        // Open modal with Selected Node
        // this.open(this.nodeModal, this.selectedNodeId);
      }

      if (e.target.closest('#editNode') === null) {
        this.hideEditButton();
      }
    });

    this.editor.on('connectionCreated', (connection: any) => {
      this.openModalForEdge(connection);
      console.log('Editor Event :>> Connection created ', connection);
    });

    this.editor.on('connectionRemoved', (connection: any) => {
      console.log('Editor Event :>> Connection removed ', connection);
    });
  }

  private dragEvent() {
    var elements = Array.from(document.getElementsByClassName('drag-drawflow'));

    elements.forEach((element) => {
      element.addEventListener('touchend', this.drop.bind(this), false);
      element.addEventListener(
        'touchmove',
        this.positionMobile.bind(this),
        false
      );
      element.addEventListener('touchstart', this.drag.bind(this), false);
      element.addEventListener('dblclick', (event) => {});
    });
  }

  private positionMobile(ev: any) {
    this.mobile_last_move = ev;
  }

  public allowDrop(ev: any) {
    ev.preventDefault();
  }

  drag(ev: any) {
    if (ev.type === 'touchstart') {
      this.selectedNode = ev.target
        .closest('.drag-drawflow')
        .getAttribute('data-node');
    } else {
      ev.dataTransfer.setData('node', ev.target.getAttribute('data-node'));
    }
  }

  drop(ev: any) {
    if (ev.type === 'touchend' && this.mobile_last_move) {
      var parentdrawflow = document
        .elementFromPoint(
          this.mobile_last_move.touches[0].clientX,
          this.mobile_last_move.touches[0].clientY
        )
        ?.closest('#drawflow');
      if (parentdrawflow != null) {
        this.addNodeToDrawFlow(
          this.mobile_item_selec,
          this.mobile_last_move.touches[0].clientX,
          this.mobile_last_move.touches[0].clientY
        );
      }
      this.mobile_item_selec = '';
    } else {
      ev.preventDefault();
      var data = ev.dataTransfer.getData('node');
      this.addNodeToDrawFlow(data, ev.clientX, ev.clientY);
    }
  }

  private addNodeToDrawFlow(name: string, pos_x: number, pos_y: number) {
    if (this.editor.editor_mode === 'fixed') {
      return false;
    }

    pos_x =
      pos_x *
        (this.editor.precanvas.clientWidth /
          (this.editor.precanvas.clientWidth * this.editor.zoom)) -
      this.editor.precanvas.getBoundingClientRect().x *
        (this.editor.precanvas.clientWidth /
          (this.editor.precanvas.clientWidth * this.editor.zoom));
    pos_y =
      pos_y *
        (this.editor.precanvas.clientHeight /
          (this.editor.precanvas.clientHeight * this.editor.zoom)) -
      this.editor.precanvas.getBoundingClientRect().y *
        (this.editor.precanvas.clientHeight /
          (this.editor.precanvas.clientHeight * this.editor.zoom));

    pos_x = pos_x - 100;

    switch (name) {
      case 'GroupCell':
        var nodeProperties = this.graphNodes.filter(
          (n) => n.id === 'GroupCell'
        )[0];
        var GroupCell = this.renderComponent(
          CaGroupComponent,
          {},
          nodeProperties
        );
        this.editor.addNode(
          'GroupCell',
          1,
          1,
          pos_x,
          pos_y,
          'GroupCell',
          {},
          GroupCell
        );
        break;
      case 'PolicyTextAnnotation':
        var nodeProperties = this.graphNodes.filter(
          (n) => n.id === 'PolicyTextAnnotation'
        )[0];
        var PolicyAnnotation = this.renderComponent(
          Annotation,
          {description: 'Default Text...', stringId: '1'},
          nodeProperties
        );
        this.editor.addNode(
          'PolicyTextAnnotation',
          0,
          0,
          pos_x,
          pos_y,
          'PolicyTextAnnotation',
          {},
          PolicyAnnotation
        );
        break;
      case 'FederationTokenGroupCell':
        var nodeProperties = this.graphNodes.filter(
          (n) => n.id === 'FederationTokenGroupCell'
        )[0];
        var FederationTokenGroupCell = this.renderComponent(
          TokenGroupComponent,
          {},
          nodeProperties
        );
        this.editor.addNode(
          'FederationTokenGroupCell',
          1,
          1,
          pos_x,
          pos_y,
          'FederationTokenGroupCell',
          {},
          FederationTokenGroupCell
        );
        break;
      case 'FederationOutgoingCell':
        var nodeProperties = this.graphNodes.filter(
          (n) => n.id === 'FederationOutgoingCell'
        )[0];
        var outgoing = this.renderComponent(
          OutgoingConnectionComponent,
          {},
          nodeProperties
        );
        this.editor.addNode(
          'FederationOutgoingCell',
          1,
          1,
          pos_x,
          pos_y,
          'FederationOutgoingCell',
          {},
          outgoing
        );
        break;
      default:
    }

    return true;
  }

  onClear() {
    this.editor.clear();
  }

  changeMode() {
    this.locked = !this.locked;
    this.editor.editor_mode =
      this.locked != null && this.locked == false ? 'edit' : 'fixed';
  }

  onZoomOut() {
    this.editor.zoom_out(0.04);
  }

  onZoomIn() {
    this.editor.zoom_in(0.04);
  }

  onZoomReset() {
    this.editor.zoom_reset();
  }

  private hideEditButton() {
    this.editButtonShown = false;
    this.editDivHtml = document.getElementById('editNode')!;
    if (this.editDivHtml) {
      this.editDivHtml.remove();
    }
  }

  private openModalForNode(node: any) {
    switch (node.name) {
      case 'PolicyTextAnnotation': {
        this.dialog.open(AddAnnotationModal, {
          disableClose: true,
          autoFocus: false,
          restoreFocus: false,
          height: '800px',
          width: '1200px',
          data: {
            onCancel: (editExisting: boolean) => {
              if (!editExisting) this.editor.removeNodeId(`node-${node.id}`);
            },
            onSave: (federation: any) => {
              var t = document.querySelector(
                ".drawflow-node[id='node-" +
                  node.id +
                  "'] .drawflow_content_node"
              );
              if (t) {
                var nodeProperties = this.graphNodes.filter(
                  (n) => n.id === node.name
                )[0];
                var GroupCell = this.renderComponent(
                  Annotation,
                  federation,
                  nodeProperties
                );
                t.innerHTML = GroupCell;
                this.editor.drawflow.drawflow.Home.data[`${node.id}`].html =
                  GroupCell;
                this.editor.drawflow.drawflow.Home.data[
                  `${node.id}`
                ].federation = federation;
              }
            },
            federation: node.federation
              ? node.federation
              : { node_id: node.id },
          },
        });
        break;
      }
      case 'GroupCell': {
        this.dialog.open(AddCAGroupModalComponent, {
          disableClose: true,
          autoFocus: false,
          restoreFocus: false,
          height: '800px',
          width: '1200px',
          data: {
            onCancel: (editExisting: boolean) => {
              if (!editExisting) this.editor.removeNodeId(`node-${node.id}`);
            },
            onSave: (federation: any) => {
              var t = document.querySelector(
                ".drawflow-node[id='node-" +
                  node.id +
                  "'] .drawflow_content_node"
              );
              if (t) {
                var nodeProperties = this.graphNodes.filter(
                  (n) => n.id === node.name
                )[0];
                var GroupCell = this.renderComponent(
                  CaGroupComponent,
                  federation,
                  nodeProperties
                );
                t.innerHTML = GroupCell;
                this.editor.drawflow.drawflow.Home.data[`${node.id}`].html =
                  GroupCell;
                this.editor.drawflow.drawflow.Home.data[
                  `${node.id}`
                ].federation = federation;
              }
            },
            federation: node.federation
              ? node.federation
              : { node_id: node.id },
          },
        });
        break;
      }
      case 'FederationTokenGroupCell': {
        this.dialog.open(AddTokenGroupModalComponent, {
          disableClose: true,
          autoFocus: false,
          restoreFocus: false,
          height: '800px',
          width: '1200px',
          data: {
            onCancel: (editExisting: boolean) => {
              if (!editExisting) this.editor.removeNodeId(`node-${node.id}`);
            },
            onSave: (federation: any) => {
              var t = document.querySelector(
                ".drawflow-node[id='node-" +
                  node.id +
                  "'] .drawflow_content_node"
              );
              if (t) {
                var nodeProperties = this.graphNodes.filter(
                  (n) => n.id === node.name
                )[0];
                var FederationTokenGroupCell = this.renderComponent(
                  TokenGroupComponent,
                  federation,
                  nodeProperties
                );
                t.innerHTML = FederationTokenGroupCell;
                this.editor.drawflow.drawflow.Home.data[`${node.id}`].html =
                  FederationTokenGroupCell;
                this.editor.drawflow.drawflow.Home.data[
                  `${node.id}`
                ].federation = federation;
              }
            },
            federation: node.federation
              ? node.federation
              : { node_id: node.id },
          },
        });
        break;
      }
      case 'FederationOutgoingCell': {
        this.dialog.open(AddOutgoingConnectionModalComponent, {
          disableClose: true,
          autoFocus: false,
          restoreFocus: false,
          height: '600px',
          width: '1200px',
          data: {
            onCancel: (editExisting: boolean) => {
              if (!editExisting) this.editor.removeNodeId(`node-${node.id}`);
            },
            onSave: (federation: any) => {
              var t = document.querySelector(
                ".drawflow-node[id='node-" +
                  node.id +
                  "'] .drawflow_content_node"
              );
              if (t) {
                var nodeProperties = this.graphNodes.filter(
                  (n) => n.id === node.name
                )[0];
                var outgoing_connection = this.renderComponent(
                  OutgoingConnectionComponent,
                  federation,
                  nodeProperties
                );
                t.innerHTML = outgoing_connection;
                this.editor.drawflow.drawflow.Home.data[`${node.id}`].html =
                  outgoing_connection;
                this.editor.drawflow.drawflow.Home.data[
                  `${node.id}`
                ].federation = federation;
              }
            },
            federation: node.federation
              ? node.federation
              : { node_id: node.id },
          },
        });
        break;
      }
      default:
        break;
    }
  }

  private openModalForEdge(connection: any) {
    this.dialog.open(AddEdgeModalComponent, {
      disableClose: true,
      autoFocus: false,
      restoreFocus: false,
      height: '800px',
      width: '600px',
      data: {
        connection,
        onSave: (edge: any) => {
          let foundConnection = this.editor.drawflow.drawflow.Home.data[
            `${edge.source}`
          ].connections.find(
            (c: any) =>
              c.source === edge.source && c.destination === edge.destination
          );
          foundConnection.federation = edge.federation;
        },
        onCancel: (editExisting: boolean) => {
          if (!editExisting)
            this.editor.removeSingleConnection(
              connection.output_id,
              connection.input_id,
              connection.output_class,
              connection.input_class
            );
        },
      },
    });
  }

  private renderComponent(component: any, federation: any, properties: any) {
    // // Create the component dynamically
    const componentRef: any = this.customPlaceholder.createComponent(component);
    // Pass data to the component via the @Input() property
    componentRef.instance.federation = federation;
    componentRef.instance.properties = properties;

    // // Manually trigger change detection to make sure the component is rendered
    this.cdr.detectChanges();

    // // Access the component's DOM element and get the outer HTML
    const htmlContent = String(componentRef.location.nativeElement.innerHTML);

    // destroy the component after rendering (if not needed anymore)
    componentRef.destroy();

    return htmlContent;
  }

  private pollActiveConnections() {
    let activeConnectionSetGroupIdentities = new Set();
    let activeConnectionSetConnectionIds = new Set();
    this.workflowService.getActiveConnections().subscribe({
      next: (activeConnections) => {
        activeConnections.forEach((activeConnection: any) => {
          activeConnection.groupIdentities.forEach((id: any) =>
            activeConnectionSetGroupIdentities.add(id)
          );
          activeConnectionSetConnectionIds.add(activeConnection.connectionId)
        });
        this.setNodeStatus(activeConnectionSetGroupIdentities, activeConnectionSetConnectionIds);
      },
      error: (e) => {
        console.log('Error getting getActiveConnections', e);
      },
    });
    this.pollingTimeoutId = setTimeout(() => this.pollActiveConnections(), 2000);
  }

  private setNodeStatus(activeConnectionSetGroupIdentities: any, activeConnectionSetConnectionIds: any) {
    Object.values(this.editor.drawflow.drawflow.Home.data).forEach((node: any) => {
        let type = node.graphType ?? node.name;
        if (type === 'FederationOutgoingCell') {
          let el = $('#node-' + node.id + ' .title-box');
          if (node.federation && activeConnectionSetConnectionIds.has(node.federation.name)) {
            if (el && el[0] && el[0] instanceof HTMLElement) {
              el[0].style.backgroundColor = '#52CC7A';
            }
          } else {
            if (el && el[0] && el[0] instanceof HTMLElement) {
              // if outgoing is enabled but there is no active connection,
              // display the node as red to indicate connection issue
              if (node.federation.outgoingEnabled)
                el[0].style.backgroundColor = 'red';
              else
                el[0].style.backgroundColor = 'var(--background-box-title)';
            }
          }
        } else {
          let el = $('#node-' + node.id + ' .title-box');
          if (node.federation && activeConnectionSetGroupIdentities.has(node.federation.name)) {
            if (el && el[0] && el[0] instanceof HTMLElement) {
              el[0].style.backgroundColor = '#52CC7A';
            }
          } else {
            if (el && el[0] && el[0] instanceof HTMLElement) {
              el[0].style.backgroundColor = 'var(--background-box-title)';
            }
          }
        }
      }
    );
  }
}
