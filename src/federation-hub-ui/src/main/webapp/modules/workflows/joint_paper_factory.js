/* globals joint */
/* globals $ */
'use strict';
angular.module('roger_federation.Workflows')
    .factory('JointPaper', ['$window',
        function($window) {
            var paper,
                graph,
                paperScroller,
                commandManager;

            function shapeSetting(width, height, preserveAspectRatio) {
                return {
                    width: width,
                    height: height,
                    preserveAspectRatio: preserveAspectRatio
                };

            }
            var options = { //All things related to default shape sizes
                maxLabelWidth: 150,
                maxPoolLabelWidth: 275,
                defaultShapeSize: {
                    'bpmn.Gateway': shapeSetting(60, 60, true),
                    'bpmn.Activity': shapeSetting(200, 150, false),
                    'bpmn.Event': shapeSetting(60, 60, true),
                    'bpmn.Annotation': shapeSetting(20, 100, false),
                    'bpmn.Pool': shapeSetting(1000, 300, false),
                    'bpmn.Group': shapeSetting(200, 200, false),
                    'bpmn.Conversation': shapeSetting(200, 200, true),
                    'bpmn.Choreography': shapeSetting(200, 200, false),
                    'bpmn.Message': shapeSetting(100, 60, true),
                    'bpmn.DataObject': shapeSetting(60, 80, true),
                    'custom.ScalableImage': shapeSetting(100, 100, true)
                },
                zoom: {
                    min: 0.2,
                    max: 4,
                    step: 0.1
                }
            };

            var autosizeCellView = function(cellView) {
                var textElement = cellView.$('text')[0]; //Find the text Element of the cellView
                // Use bounding box without transformations so that our autosizing works
                // even on e.g. rotated element.
                var bbox = V(textElement).bbox(true);
                // Give the element some padding on the right/bottom.
                cellView.model.resize(Math.max(bbox.width + 50, 50), Math.max(bbox.height + 5, 30));
            };

            var openInlineEditor = function(cellView) {
                var textElement = cellView.$('text')[0]; //Find the text Element of the cellView
                var editor = joint.ui.TextEditor.edit(textElement, {
                    cellView: cellView,
                    textProperty: 'content',
                    useNativeSelection: false
                });
                if (editor !== undefined) {
                    editor.on('text:change', function(newText) {
                        autosizeCellView(cellView);
                    });
                }
            };
            var cleanUpInlineEditor = function() {
                joint.ui.TextEditor.deselect(); //Deselect contents of inline text editor
                joint.ui.TextEditor.close(); //Close inline text editor
            };

            var validateConnection = function(cellViewS, magnetS, cellViewT, magnetT, end, linkView) {
                // Prevent linking from output ports to input ports within one element.
                if (cellViewS === cellViewT) {
                    return false;
                }
                return true;
            };

            return {
                init: function(diagramType, jointDiagramId, jointPaperId, jointStencilId, width, height, gridSize) {
                    graph = new joint.dia.Graph({
                        type: 'bpmn'
                    });
                    this.graph = graph;
                    graph.clear();
                    paper = new $window.joint.dia.Paper({
                        el: $(jointDiagramId).$el,
                        width: width,
                        height: height,
                        gridSize: gridSize,
                        model: graph,
                        interactive: {
                            vertexAdd: false
                        },
                        perpendicularLinks: true,
                        validateConnection: validateConnection,
                        snapLinks: {
                            radius: 100
                        },
                        linkPinning: false,
                        defaultLink: new joint.shapes.bpmn.Flow({
                            roger_federation: {
                                type: 'Flow',
                                priority: 0,
                                timer: {
                                    value: 0,
                                    units: "minutes",
                                    description: ""
                                }
                            },
                            graphType: "EdgeCell",
                            router: {
                                name: 'metro'
                            },
                            connector: {
                                name: 'rounded'
                            }
                        }),
                        markAvailable: true,
                        embeddingMode: true,
                        validateEmbedding: function(childView, parentView) {
                            var parentType = parentView.model.attributes.type;
                            var childType = childView.model.attributes.type;

                            if (parentType === "bpmn.Pool" && (childType !== "bpmn.Pool" && childType !== "bpmn.Group")) {
                                return true;
                            } else if (parentType === "bpmn.Group" && childType !== "bpmn.Group") {
                                return true;
                            }
                            return false;
                        }
                    }).on({
                        'blank:pointerdown': function(evt, x, y) {
                            paperScroller.startPanning(evt, x, y);
                        }
                    });
                    this.paper = paper;

                    commandManager = new joint.dia.CommandManager({
                        graph: this.graph
                    });
                    this.commandManager = commandManager;

                    /* Paper Scroller / Mouse Wheel zoom */
                    paperScroller = new joint.ui.PaperScroller({
                        autoResizePaper: true,
                        padding: 200,
                        paper: paper
                    });
                    this.paperScroller = paperScroller;

                    paperScroller.$el.appendTo(jointPaperId);
                    paperScroller.$el.height("100%");
                    paperScroller.center();

                    /* Zoom Scroll */
                    function onMouseWheel(e) {
                        e.preventDefault();
                        e = e.originalEvent;
                        var delta = e.wheelDelta ? e.wheelDelta : -e.detail;
                        if (delta > 0) {
                            paperScroller.zoom(options.zoom.step, {
                                max: options.zoom.max
                            });
                        } else {
                            paperScroller.zoom(-options.zoom.step, {
                                min: options.zoom.min
                            });
                        }
                    }
                    paper.$el.on('mousewheel DOMMouseScroll', onMouseWheel);

                    // Disable context menu inside the paper.
                    // This prevents from context menu being shown when selecting individual elements with Ctrl in OS X.
                    paper.el.oncontextmenu = function(evt) {
                        evt.preventDefault();
                    };

                    // Resize shapes when they are dropped on the canvas from the stencil.
                    graph.on({
                        'add': function(cell, collection, opt) {
                            var type = cell.get('type');

                            if (!opt.stencil) {
                                return;
                            }

                            //Adds the Gateway and Event type to the shape label.  Helpful for BPMN newbies
                            if (type === "bpmn.Event" || type === "bpmn.Gateway") {

                                cell.attributes.attrs['.label'].text = joint.util.breakText( cell.attributes.roger_federation.type, {
                                    width: options.maxLabelWidth
                                }) ;
                            }

                            var defaultShapeSize = options.defaultShapeSize[type];
                            if (defaultShapeSize) {
                                cell.set('size', {
                                    width: defaultShapeSize.width,
                                    height: defaultShapeSize.height
                                }, {
                                    silent: false
                                });
                            }


                        }

                    });

                    //Custom Icons
                    joint.shapes.bpmn.icons.dataStore = "data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiA/PjwhRE9DVFlQRSBzdmcgIFBVQkxJQyAnLS8vVzNDLy9EVEQgU1ZHIDEuMS8vRU4nICAnaHR0cDovL3d3dy53My5vcmcvR3JhcGhpY3MvU1ZHLzEuMS9EVEQvc3ZnMTEuZHRkJz48c3ZnIGVuYWJsZS1iYWNrZ3JvdW5kPSJuZXcgMCAwIDI0IDI0IiBoZWlnaHQ9IjI0cHgiIGlkPSJMYXllcl8xIiB2ZXJzaW9uPSIxLjEiIHZpZXdCb3g9IjAgMCAyNCAyNCIgd2lkdGg9IjI0cHgiIHhtbDpzcGFjZT0icHJlc2VydmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiPjxwYXRoICBkPSJtIDAuOTA3NzQ0NTUsNC4wMTI4NTI1IDAsMTYuMjM1MDExNSBjIDAuNzQwODE4OTUsMy42MDc3ODEgMjEuNDgzNzUxNDUsMy42MDc3ODEgMjIuMjI0NTcwNDUsMCBsIDAsLTE2LjIzNTAxMTUgYyAtMC43NDA4MTksLTMuNjA3NzgwMTIgLTIxLjQ4Mzc1MTUsLTMuNjA3NzgwMTIgLTIyLjIyNDU3MDQ1LDAgMC43NDA4MTg5NSwzLjYwNzc4MDEgMjEuNDgzNzUxNDUsMy42MDc3ODAxIDIyLjIyNDU3MDQ1LDAgTSAwLjkwNzc0NDU1LDYuNTM4Mjk4OCBjIDAuNzQwODE4OTUsMy42MDc3ODAyIDIxLjQ4Mzc1MTQ1LDMuNjA3NzgwMiAyMi4yMjQ1NzA0NSwwIE0gMC45MDc3NDQ1NSw5LjA2Mzc0NDkgYyAwLjc0MDgxODk1LDMuNjA3NzgwMSAyMS40ODM3NTE0NSwzLjYwNzc4MDEgMjIuMjI0NTcwNDUsMCINCiAgICAgc3R5bGU9ImZpbGw6I2ZmZmZmZjtzdHJva2U6IzAwMDAwMDtzdHJva2Utd2lkdGg6MC44OTE5NzE0MSIgLz48L3N2Zz4=";
                    joint.shapes.bpmn.icons.outgoing = 'data:image/png;base64,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'
                    joint.shapes.bpmn.icons.federation = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAWAgMAAACnE7QbAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAYdEVYdFNvZnR3YXJlAHBhaW50Lm5ldCA0LjAuOWwzfk4AAAAJUExURQAAAP///wAAAHPGg3EAAAACdFJOUwAAdpPNOAAAACdJREFUCNdjYGBatYqBgYkBDIinQCwHOI9xFapcA4iNkLvWtIJ0GwDTEwWTtBTeUwAAAABJRU5ErkJggg==";
                    joint.shapes.bpmn.icons.eventGateway = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAMAAACdt4HsAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAYdEVYdFNvZnR3YXJlAHBhaW50Lm5ldCA0LjAuOWwzfk4AAAB1UExURf///wAAAAEBAQsLCw0NDRERERcXFxgYGBwcHCMjIyQkJCkpKSoqKisrKy8vLzAwMDExMTMzMzU1NTk5OTs7O0JCQkNDQ0pKSktLS1FRUVJSUlRUVFlZWVpaWl5eXmFhYWNjY2lpaXNzc3x8fIeHh7q6utvb22ODD/wAAAABdFJOUwBA5thmAAAD10lEQVRYw5VXW3viOgzUbC4kbeiyZcPCNtTF9pn//xPPg3xLSKDrB39tsGXNjCVLIhuD3vuvr+vpdP368t5T/mnQXzvMRnf9vg26vw1WRvPX8VuH79OW6vVtHN9eq/Rh/9QN+l/RZ5OA0/spIvr12ARHXTbY5Tp6O+hv4wMLPOh2Tz03q6Af1MRh0wJ3ANA7itBddyWBu6t+7QFgxw3yawD4pAjNigyNoQg/AaBek4NnAGg9hbZeV6G2FPoWAM53FtT0QKELvve3rMKtD0gchUN0dDYcABwZHEG7kIHetghH8wgAboW/Y/yxsWsYbYVi0ZxJ9ioP94+U1luyp8rdc0FgR7VTEsRpOC3Xoaewm6/zAKpguMoSKZ+lZnRVcLUC4NPnAYBVaBUX5yns/LFSjFY1ywoMVCEyucrHDwDoCknCKvZ5MXsAXtiUuOgaADj8N2LOS+TLZx69wjJlnISw/GC0tOcMsFHCfApBL6yBJl29rvBcr16TuGQD1IznqgODOmDDio954Ac20/9WXeiDC05JyQ5oVqhm6nUA8MKsRM2wUWQC4MUBmLL4WaKw5xjDUcIWJ163sAN6yiX4E9y9C9ccCQH1RdgnOYywVVHU/XY1YfRQAfXPVmnz4pM7t8DPVtJTaScRkVuC7cPftmB0M+3yMxwjHoANpxqgYphV42Ez65p0dyrAhHkCXkUM8Coikshc9aALHIi8AibMIzCK/AHeU6zYDQM+UiDyDvwR+Q2MwYDOAcNlw8Ate1fsWhiQSw6JFQQt7wxcgHcJc7zo7hkCeQOmMJs5ieGWPUFQkjiXUYN3FUOJoJTRFZfqIYYSgU+XuJxvjzGUCG6LrTmYtjGEACrRaCII12t6hmGJYEoXMycU+whDadYWvsfIYpM85JCdlTIXp5zbAg0TKSEAi6RqAdx4j+AyT6rpHA3AkCnjYtTDbNTFQ1QDFQuT4WWzKZeww2qlypyYrL4WLj81TtimZGpXDZj8SLSMxyaX+/JxpTP3I6T08nH1ZX35SZ6RaNiuRWsAZ/Jjlnw9lMfjVg04ryWP6m1ZSoxaNekzet4uZc+xFtwlTgoQQ7TwuMiKy+avB9uyzKvXy7y6LPOWd5XVrNDc3Reau1mhWd0d4WIN6LQkxUtZ6oar1bpYS7oNgnae8SEGUA8/T6efQ6q9bSql14jWGvBZuf+xLD7umMSLthZTW+5uJ08Ruhes8HfX8uxjy2PMNI6TMbHl2T9peXLTtV9runR7ZR63bf6QW9Wi7UuN7OFp7xl7sxUVYj/3vPW9rLe+F/f99jm3qrmR/ecGvlRhe/f/2BV4PKllnhkAAAAASUVORK5CYII=";
                    joint.shapes.bpmn.icons.messageThrow = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAVBAMAAADV4/HZAAAABGdBTUEAALGPC/xhBQAAAAlwSFlzAAAOwgAADsIBFShKgAAAABh0RVh0U29mdHdhcmUAcGFpbnQubmV0IDQuMC45bDN+TgAAABJQTFRF////AAAAEBAQICAgMDAwQEBA9ql18wAAAAF0Uk5TAEDm2GYAAABVSURBVBjT1Y25EYAwEAOXL2fNkEMHpgQ6gP6bIfAc9tABirQanQ4mG2XIDJU3wIs+eAZR6AqvsIsmuFUXOJXiDk2lSe2WS2Ittnn/jbaBOQx+9J/gAcYZFZNx+7SlAAAAAElFTkSuQmCC";
                    joint.shapes.bpmn.icons.timer = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGEAAABhCAMAAADx2D+RAAAABGdBTUEAALGPC/xhBQAAAAlwSFlzAAAOwgAADsIBFShKgAAAABh0RVh0U29mdHdhcmUAcGFpbnQubmV0IDQuMC45bDN+TgAAAFpQTFRF////AAAABwcHCAgICgoKDw8PEBAQERERHBwcHR0dHh4eHx8fICAgKioqLy8vMDAwNzc3Ozs7Pz8/QEBAT09PUFBQWlpaX19fYGBgb29vcHBwf39/gICAj4+PemnMMAAAAAF0Uk5TAEDm2GYAAAPzSURBVGjetVrpgqMgDNbdxT10cQbpUofy/q+5tfUAkkCgNn86I5ivuUNo0xTR7UHNG+g2jW1I3fR1HveY+UHD9QT2Q5um4TVRLi2Hpmr+U8ul4c38FxrL9d+WUqHRMfsKdaf7p1RKYBDdKwIIbZ8r9v7P+qeT9WJEFhDmWPIQFtKiyhpd8JIK1iKEO6lyTQUAH9EiRGiazwCjVIKJgWDbIojYeASC88CXbd/YEIEBFIzWDUEfOWnZ1a8P8hCeiuyK51AEj9n6MbMgumiTAd4BEfrd3xgQRyCL9cni7jOCYHZNOo+hyDntBQA83QRB2IR1j7/2iJTp0HMQ4KmDwGOtDZRkwu0ymUBwLaJa3awqmzaKD5EwxYCvKay+bIV7fnoq+j2BKY7AnIFomhL3IQylCkM5asxulqTFFgMoRWgwht4TtmTkRk18y3A18qft6TdO9s3E1U9sfSrqGtIIA8qrqPsxhMVgfYTv8MrtEdC5Aj8CEX6vPtvd6pS0YpvPeIvzHuisOWbK68ajsMcxMQbvyIw5Rjwy16/WBx7bEWKLFiSDvJIePYew6CYH3pkTRcTBBLwzNSADmqDj8RULkkE+oGeFePQQSGSbEwIa3cbvpSoRNsX2LIBkQCPNpvHeMUVNrcvsc17bq96hpH1j530rFsDMLiNRz8ZGIAL6RAT+5r4OgQxo2pnsjsAp0PtrUw1CfzuI0fxfEqObB33sCA45t1rW+QVONCzKyxYgGLDzwkBoChC6zHAmjdDbg7iHPCCKx+MDIHAO207x50zQl5h5qTE9hXEWwqILxUAYKrNGQhRN5hdRhQCt4kgEVYsQiYL7nSiucaQoPV3jXMWICxFlThSS9hU1sQqJYNetWgSNRPVUPqU1A9IZ9kTfeq3RWVCYutB9IoRbVzMGfnrUF17PVdDGDrymjuhzuuA01yNmmQqqNl6ZR4/hHCOIf+xWItXBXP4Cy0bVsd5traBmtfQIt9hniXx+PO9fjrUj40q0Ap8Qzj9QXrr+poJqnmN/FNRYw6RDb4z3Xyl1OGqqkexSNWDkaI+U9JyvT1p1wFsqJKYEf1YZLlu0K0x2dF3Yk+iou4471Q6bXs3pYOlyM+NQcgMBiLwmQTR2MUN4wxHtziQ2gczuReYORe5B1OYBAogrambklmbddGVecQh/SCehQhEE/TDcyL6mEbx7oPiV7wVXTSGEOf8uK4L4xUD4E1xAcpKjTFxEQoTwipNZfw194IwQ4jtsfn0Et8PbDxs8hK+Bfw7PampT2DQtXIcJPfWWFmAnSu/YbXE11EX86zoU82b+DwzxXv60zT2SrnmZnCYlUbY5i+wc/4JC6fO4h1C2lPF/NlQ1aBrowPMAAAAASUVORK5CYII=";
                    joint.shapes.bpmn.icons.error = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADgAAAA1CAMAAAAjxtjyAAAABGdBTUEAALGPC/xhBQAAAAlwSFlzAAAOwgAADsIBFShKgAAAABh0RVh0U29mdHdhcmUAcGFpbnQubmV0IDQuMC45bDN+TgAAADNQTFRF////AAAADw8PEBAQHx8fICAgLy8vMDAwPz8/QEBAT09PUFBQX19fYGBgb29vf39/j4+Pg4tmAgAAAAF0Uk5TAEDm2GYAAAFbSURBVEjHnVbbooMgDDNyETlM/f+vPQ9uU2laGH0EQ9JQaadpJF7TYABxCLcC2yAh6PrSJqTA0tIBIP0i5E64k/UDgGsQ0pMDACw2IbQDgZdJmKg1Z5iEB9lwb6AzCA2laporAMzs8r9AniYAIFuEPJNF2yh3ILRzDWvOCPSOaYp4xh/dzopneppBVQoRbFviNgl0ktC3rDljFYSlR+n91wuq0pUCUR/bSfhN80Poe6y5lzvUFIMGxHbfNZV6maZewPn6bq5lu4swWoTiN4F7yH7EXhVohH05xJp38ftO4GXMZ2VmuKhaM9sFsamEjVJiDUPWxdYGBp5CqXFJUSqeoaj9Y82G4W2lmmXiUrg1cZoaD1jqfmcr5NH7sNeXQq3Z1R7Mqup6FZMxLkTwt99K8GHtQXpeY5iaWYraNNBSVZTeLJFZWOM7JsbClHbNmhmiYXROqZVSX8am63+m4AfV+lOQ5gAAAABJRU5ErkJggg==";
                    joint.shapes.bpmn.icons.cancel = joint.shapes.bpmn.icons.cross;
                    joint.shapes.bpmn.icons.compensation = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADQAAAAnCAMAAAB35JvsAAAABGdBTUEAALGPC/xhBQAAAAlwSFlzAAAOwgAADsIBFShKgAAAABh0RVh0U29mdHdhcmUAcGFpbnQubmV0IDQuMC45bDN+TgAAADlQTFRF////AAAADw8PEBAQHx8fICAgLy8vMDAwPz8/QEBAT09PUFBQX19fYGBgb29vcHBwf39/gICAj4+PR62fbAAAAAF0Uk5TAEDm2GYAAADjSURBVEjHndXLEoJADETRbgaY8FDQ//9YVypgdxRZ5tSdmlQhAv6Jk3MAA+PUHAAKNbo5gIWUOJk5AFRq7OijQokLaaOJGit91FFjoY1upMTBzI+2xUIf7eyNF9JGB3thRx9VSryRPqLGSh8N1FjoI2UMedQzWqSxK/RR5bnHXftLtJLnI8z/REDzT4TQ2KSRWSzQZhEkBzCmkWK38PYtb35dePd76jXe0whXg30WHa4Y5rSPr1EYbLNou8EOxyzabBDm6vJbPhuMLHoeGubq9h/SYJ9FWA1eswhoDTZZhDHc/AHyighWi2ezrQAAAABJRU5ErkJggg==";
                    joint.shapes.bpmn.icons.conditional = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACoAAAAzBAMAAAATNIc7AAAABGdBTUEAALGPC/xhBQAAAAlwSFlzAAAOwgAADsIBFShKgAAAABh0RVh0U29mdHdhcmUAcGFpbnQubmV0IDQuMC45bDN+TgAAAB5QTFRF////AAAAHx8fICAgPz8/QEBAf39/n5+fv7+/z8/Pp5BJAQAAAAF0Uk5TAEDm2GYAAABUSURBVDjLYwjFAiIZBLEACQZBFRd0oAgUTWBAB4k0FS1FcWwBVDQRxbEJUNESFNcW0Nhlw8O9rCjuZcDrXhYU9zKMhu+wc68pRvFgKIGjfHDBAjwB20l50xdUDe0AAAAASUVORK5CYII=";
                    joint.shapes.bpmn.icons.signalStart = "data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiA/PjwhRE9DVFlQRSBzdmcgIFBVQkxJQyAnLS8vVzNDLy9EVEQgU1ZHIDEuMS8vRU4nICAnaHR0cDovL3d3dy53My5vcmcvR3JhcGhpY3MvU1ZHLzEuMS9EVEQvc3ZnMTEuZHRkJz48c3ZnIGVuYWJsZS1iYWNrZ3JvdW5kPSJuZXcgMCAwIDI0IDI0IiBoZWlnaHQ9IjI0cHgiIGlkPSJMYXllcl8xIiB2ZXJzaW9uPSIxLjEiIHZpZXdCb3g9IjAgMCAyNCAyNCIgd2lkdGg9IjI0cHgiIHhtbDpzcGFjZT0icHJlc2VydmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiPjxwYXRoDQogICAgIGlkPSJwYXRoMzM1OCINCiAgICAgZD0iTSAxMi4wNjA3ODksMS45NjEzNDA2IDEuODM2MDQsMTkuNzcxNDk0IDIyLjEyNDc3MywxOS43MTM5NTIgWiINCiAgICAgc3R5bGU9ImZpbGw6bm9uZTtmaWxsLXJ1bGU6ZXZlbm9kZDtzdHJva2U6IzAwMDAwMDtzdHJva2Utd2lkdGg6MS45NDQ0NzA4ODtzdHJva2UtbGluZWNhcDpidXR0O3N0cm9rZS1saW5lam9pbjptaXRlcjtzdHJva2UtbWl0ZXJsaW1pdDo0O3N0cm9rZS1kYXNoYXJyYXk6bm9uZTtzdHJva2Utb3BhY2l0eToxIiAvPjwvc3ZnPg==";
                    joint.shapes.bpmn.icons.signalEnd = "data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiA/PjwhRE9DVFlQRSBzdmcgIFBVQkxJQyAnLS8vVzNDLy9EVEQgU1ZHIDEuMS8vRU4nICAnaHR0cDovL3d3dy53My5vcmcvR3JhcGhpY3MvU1ZHLzEuMS9EVEQvc3ZnMTEuZHRkJz48c3ZnIGVuYWJsZS1iYWNrZ3JvdW5kPSJuZXcgMCAwIDI0IDI0IiBoZWlnaHQ9IjI0cHgiIGlkPSJMYXllcl8xIiB2ZXJzaW9uPSIxLjEiIHZpZXdCb3g9IjAgMCAyNCAyNCIgd2lkdGg9IjI0cHgiIHhtbDpzcGFjZT0icHJlc2VydmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiPjxwYXRoICBkPSJNIDEyLjA2MDc4OSwxLjk2MTM0MDYgMS44MzYwNCwxOS43NzE0OTQgMjIuMTI0NzczLDE5LjcxMzk1MiBaIiAvPjwvc3ZnPg==";
                    joint.shapes.bpmn.icons.multiple = "";
                    joint.shapes.bpmn.icons.linkCatch = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEEAAAAuCAMAAABAi4T4AAAABGdBTUEAALGPC/xhBQAAAAlwSFlzAAAOwgAADsIBFShKgAAAABh0RVh0U29mdHdhcmUAcGFpbnQubmV0IDQuMC45bDN+TgAAADlQTFRF////AAAADw8PEBAQHx8fICAgLy8vMDAwPz8/QEBAT09PUFBQX19fYGBgb29vcHBwf39/gICAj4+PR62fbAAAAAF0Uk5TAEDm2GYAAADwSURBVEjHpdZbcoMwEETRvsjmFRCY/S/WHzhG2KkA07OAU3ARqpFOTJI74AuMtkBjCyRPyC1WDJiVrRgwS6qMGKugWzzGS1ALPCxBGfixhHCMQlD9FYNTswnqP2NcFjQD3U5o5+PZP3cF97/f8fTsYoQE9cBkCVq2GEGhiBEW3jHigob1hzcELUBjCUpQO8IE9M5bdK/THhbuUFlfM0HtnKgJGJxT3QHL9meN8QTBG0a8E8SEsUiw3lnHM5RCUyaI3NVplyAgfCS4LHwluCo08R1iFYIJNiGa4HcLGoFsbmKV5Ak3dxts3Y00e4D+TfAEPfQKajvsB4UAAAAASUVORK5CYII=";
                    joint.shapes.bpmn.icons.linkThrow = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEIAAAAuCAMAAACrvD/7AAAABGdBTUEAALGPC/xhBQAAAAlwSFlzAAAOwgAADsIBFShKgAAAABh0RVh0U29mdHdhcmUAcGFpbnQubmV0IDQuMC45bDN+TgAAADlQTFRF////AAAADw8PEBAQHx8fICAgLy8vMDAwPz8/QEBAT09PUFBQX19fYGBgb29vcHBwf39/gICAj4+PR62fbAAAAAF0Uk5TAEDm2GYAAAC8SURBVEjHrdbREoIgEIVhzlqkBkW9/8N6oSk0kzn7u7fOfAxHYDeEA1VyoFV044Q6TkgnELpzAgWyECSQDyG9OOEOpCK8gdSEM5CGkJ4OIreEht0VjtWVE20gPqIJxEtUgbiJbTN+Yr3/hFDmxHzcGaGOE9IJxIMShjcScZwj/qmFHi3DBzziazbiy17ok2Nfj+f/6n/G4G0CCfeRN21FhhtixG054eEAjyiGB6WIx7UUKIFH1wseoPu9rxNwgAugfbSzAwAAAABJRU5ErkJggg==";
                    joint.shapes.bpmn.icons.terminate = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAMAAACdt4HsAAAABGdBTUEAALGPC/xhBQAAAAlwSFlzAAAOwgAADsIBFShKgAAAABh0RVh0U29mdHdhcmUAcGFpbnQubmV0IDQuMC45bDN+TgAAADlQTFRF////AAAADw8PEBAQHx8fICAgLy8vMDAwPz8/QEBAT09PUFBQX19fYGBgb29vcHBwf39/gICAj4+PR62fbAAAAAF0Uk5TAEDm2GYAAAEQSURBVBgZpcEHYoMwEEXB9xHGCxaO4P6HjWviQpG0M2yZpr7vx2miwtTrzWGkwBS05ESek1Yd2PejTT07gvb0bDgpQ2DVUVkCK4JyTSwJyjfyLajEyKejyvChVynejCoWeKUKR/4F1Ug8JVUJPAXVSdxF1eJO1SJXZ9Xj6qB6XMkhArMcAmDyAORyBrkYyMWY5XIgyYckH0w+mHyI8iHJhyQfkIuBXAzkMkArD+AsDy7k0HEhh5mLQfW4UbWWm1a1eFAl48FUhz+qYvwZVIMXjcrNvGpUKvJOhTo+JBVp+RJVoGFBVLaWRbMydaxplCOyrtO+mS1zo23GnkEbjBznRstaspm+2EyZZJ3uGrPIml/dVBBSEkQZawAAAABJRU5ErkJggg==";

                    //This is a generic shape that will be used as the container for our custom icons. for exampel Data Store
                    joint.shapes.custom = {};
                    joint.shapes.custom.ScalableImage = joint.dia.Element.extend({
                        markup: '<g class="rotatable"><g class="scalable"><polygon class="body"/><image/></g><text class="label"/></g>',
                        defaults: joint.util.deepSupplement({
                            type: "custom.ScalableImage",
                            attrs: {
                                ".body": {
                                    points: "40,40,0,40",
                                    // stroke: "#000000",
                                    fill: "#ffffff"
                                },
                                ".label": {
                                    ref: ".body",
                                    "ref-x": 0.5,
                                    "ref-dy": 5,
                                    text: "",
                                    "text-anchor": "middle"
                                },
                                image: {
                                    width: 40,
                                    height: 40,
                                    "xlink:href": "",
                                    transform: "translate(0,0)"
                                }
                            }
                        }, joint.dia.Element.prototype.defaults)
                    }).extend(joint.shapes.bpmn.IconInterface);

                    //Increase the default size of the pool header
                    joint.shapes.bpmn.PoolView.prototype.options.headerWidth = 60;

                    /* beautify preserve:start */
                    /* STENCIL */
                    var groups = {};
                    if (diagramType === "Workflow") {
                        groups = {
                            participants: { label: 'Participants', index: 1 },
                            activities: { label: 'Activities', index: 2 },
                            events: { label: 'Events', index: 3 },
                            gateways: { label: 'Gateways', index: 4 },
                            data: { label: 'Data', index: 5 },
                            artifacts: { label: 'Artifacts', index: 6 }
                        };
                    } else if (diagramType === "Federation") {
                        groups = {
                            participants: { label: 'Participants', index: 1 }
                        };
                    }

                    var stencil = new joint.ui.Stencil({
                        graph: graph,
                        paper: paper,
                        width: 200,
                        height: 300,
                        groups: groups,
                        //  search: {
                        //      '*': ['roger_federation/type']
                        //  }
                    });

                    stencil.render().$el.appendTo(jointStencilId);

                    var addStencilToolip = function (stencilGraph) {
                        stencilGraph.get('cells').each(function(cell) {
                            var toolTipMessage = cell.attributes.roger_federation.type;
                            if (toolTipMessage === undefined) {
                                toolTipMessage = cell.get('type').split('.')[1];
                            }
                            $('.stencil [model-id="' + cell.id + '"]').popover({
                                placement: 'auto',
                                trigger: 'hover',
                                html: false,
                                delay: {
                                    "show": 50,
                                    "hide": 0
                                },
                                container: 'body',
                                content: toolTipMessage
                            });
                        });
                    };

                    if (diagramType === "Workflow") {
                        //Participants group
                        stencil.load([new joint.shapes.bpmn.Pool({
                            position: { x: 10, y: 10 }, size: { width: 175, height: 60 },
                            roger_federation: {
                                type: "Pool",
                                priority: 0,
                                missionCritical: false
                            },
                            attrs: {
                                '.': {
                                    magnet: false
                                },
                                '.header': {
                                    // fill: '#5799DA'
                                },
                                ".blackbox-label": {
                                    text: "",
                                }
                            },
                            lanes: {
                                label: 'Pool',
                                sublanes: []
                            }
                        })], 'participants');
                        addStencilToolip(stencil.graphs.participants);
                        //Activities group
                        var c = [0, 10, 60, 110, 160];
                        // var r = [0, 10, 60, 110, 160, 210, 260, 310, 360];
                        // var eventSize =  { width: 40, height: 40 };
                        var r = [0, 5, 45, 85, 125, 165, 205, 245, 285, 325];
                        var eventSize =  { width: 35, height: 35 };
                        stencil.load([
                            new joint.shapes.bpmn.Activity({
                                position: { x: 10, y: 10 }, size: { width: 80, height: 80 },
                                roger_federation: {
                                    type: "Product",
                                    priority: 0,
                                    missionCritical: false
                                },
                                icon: "message",
                                activityType: "call-activity"
                            }), //Contains foreignObject
                            new joint.shapes.bpmn.Activity({
                                position: { x: 100, y: 10 }, size: { width: 80, height: 80 },
                                roger_federation: {
                                    name: "",
                                    type: "Sub-Process",
                                    priority: 0,
                                    missionCritical: false
                                },
                                subProcess: true,
                                activityType: "transaction"
                            })

                        ], 'activities');
                        addStencilToolip(stencil.graphs.activities);
                        //events group
                        stencil.load([
                            //Plain
                            new joint.shapes.bpmn.Event({
                                position: { x: c[1], y: r[1] }, size: eventSize,
                                roger_federation: {
                                    type: "Start Event"
                                },
                                eventType: "start"
                            }),
                            new joint.shapes.bpmn.Event({
                                position: { x: c[3], y: r[1] }, size: eventSize,
                                roger_federation: {
                                    type: "Intermediate Throw Event"
                                },
                                eventType: "intermediate"
                            }),
                            new joint.shapes.bpmn.Event({
                                position: { x: c[4], y: r[1] }, size: eventSize,
                                roger_federation: {
                                    type: "End Event"
                                },
                                eventType: "end"
                            }),
                            //Message
                            new joint.shapes.bpmn.Event({
                                position: { x: c[1], y: r[2] }, size: eventSize,
                                roger_federation: {
                                    type: "Message Start Event"
                                },
                                eventType: "start",
                                icon: "message"
                            }),
                            new joint.shapes.bpmn.Event({
                                position: { x: c[2], y: r[2] }, size: eventSize,
                                roger_federation: {
                                    type: "Message Intermediate Catch Event"
                                },
                                eventType: "intermediate",
                                icon: "message"
                            }),
                            new joint.shapes.bpmn.Event({
                                position: { x: c[3], y: r[2] }, size: eventSize,
                                roger_federation: {
                                    type: "Message Intermediate Throw Event"
                                },
                                eventType: "intermediate",
                                icon: "messageThrow"
                            }),
                            new joint.shapes.bpmn.Event({
                                position: { x: c[4], y: r[2] }, size: eventSize,
                                roger_federation: {
                                    type: "Message End Event"
                                },
                                eventType: "end",
                                icon: "messageThrow"
                            }),
                            //Signal
                            new joint.shapes.bpmn.Event({
                                position: { x: c[1], y: r[3] }, size: eventSize,
                                roger_federation: {
                                    type: "Signal Start Event"
                                },
                                icon: "signalStart",
                                eventType: "start"
                            }),
                            new joint.shapes.bpmn.Event({
                                position: { x: c[2], y: r[3] }, size: eventSize,
                                roger_federation: {
                                    type: "Signal Intermediate Catch Event"
                                },
                                icon: "signalStart",
                                eventType: "intermediate"
                            }),
                            new joint.shapes.bpmn.Event({
                                position: { x: c[3], y: r[3] }, size: eventSize,
                                roger_federation: {
                                    type: "Signal Intermediate Throw Event"
                                },
                                icon: "signalEnd",
                                eventType: "intermediate"
                            }),
                            new joint.shapes.bpmn.Event({
                                position: { x: c[4], y: r[3] }, size: eventSize,
                                roger_federation: {
                                    type: "Signal End Event"
                                },
                                icon: "signalEnd"
                            }),
                            // Timer
                            new joint.shapes.bpmn.Event({
                                position: { x: c[1], y: r[4] }, size: eventSize,
                                roger_federation: {
                                    type: "Timer Start Event"
                                },
                                eventType: "start",
                                icon: "timer"
                            }),
                            new joint.shapes.bpmn.Event({
                                position: { x: c[2], y: r[4] }, size: eventSize,
                                roger_federation: {
                                    type: "Timer Intermediate Catch Event"
                                },
                                eventType: "intermediate",
                                icon: "timer"
                            }),
                            //Error
                            new joint.shapes.bpmn.Event({
                                position: { x: c[4], y: r[5] }, size: eventSize,
                                roger_federation: {
                                    type: "Error End Event"
                                },
                                eventType: "end",
                                icon: "error"
                            }),
                            //Cancel
                            new joint.shapes.bpmn.Event({
                                position: { x: c[4], y: r[6] }, size: eventSize,
                                roger_federation: {
                                    type: "Cancel End Event"
                                },
                                eventType: "end",
                                icon: "cancel"
                            }),
                            //Compensation
                            new joint.shapes.bpmn.Event({
                                position: { x: c[3], y: r[7] }, size: eventSize,
                                roger_federation: {
                                    type: "Compensation Intermediate Throw Event"
                                },
                                eventType: "intermediate",
                                icon: "compensation"
                            }),
                            new joint.shapes.bpmn.Event({
                                position: { x: c[4], y: r[7] }, size: eventSize,
                                roger_federation: {
                                    type: "Compensation End Event"
                                },
                                eventType: "end",
                                icon: "compensation"
                            }),
                            // Conditional
                            new joint.shapes.bpmn.Event({
                                position: { x: c[1], y: r[8] }, size: eventSize,
                                roger_federation: {
                                    type: "Conditional Start Event"
                                },
                                eventType: "start",
                                icon: "conditional"
                            }),
                            new joint.shapes.bpmn.Event({
                                position: { x: c[2], y: r[8] }, size: eventSize,
                                roger_federation: {
                                    type: "Conditional Intermediate Catch Event"
                                },
                                eventType: "intermediate",
                                icon: "conditional"
                            }),
                            //terminate,
                            new joint.shapes.bpmn.Event({
                                position: { x: c[4], y: r[9] }, size: eventSize,
                                roger_federation: {
                                    type: "Terminate End Event"
                                },
                                eventType: "end",
                                icon: "terminate"
                            }),




                        ], 'events');
                        addStencilToolip(stencil.graphs.events);
                        //Gateways group
                        stencil.load([
                            new joint.shapes.bpmn.Gateway({
                                position: { x: c[1], y: r[1] }, size: eventSize,
                                roger_federation: {
                                    type: "Inclusive Gateway"
                                },
                                icon: "circle"
                            }),
                            new joint.shapes.bpmn.Gateway({
                                position: { x: c[2], y: r[1] }, size: eventSize,
                                roger_federation: {
                                    type: "Exclusive Gateway"
                                },
                                icon: "cross"
                            }),
                            new joint.shapes.bpmn.Gateway({
                                position: { x: c[3], y: r[1] }, size: eventSize,
                                roger_federation: {
                                    type: "Parallel Gateway"
                                },
                                icon: "plus"
                            }),
                            new joint.shapes.bpmn.Gateway({
                                position: { x: c[4], y: r[1] }, size: eventSize,
                                roger_federation: {
                                    type: "Event Gateway"
                                },
                                icon: "eventGateway"
                            })
                        ], 'gateways');
                        addStencilToolip(stencil.graphs.gateways);
                        //Data group
                        stencil.load([
                            new joint.shapes.bpmn.DataObject({
                                position: { x: 30, y: 10 }, size: { width: 50, height: 60 },
                                roger_federation: {
                                    type: "Data Object"
                                }
                            }),
                            new joint.shapes.custom.ScalableImage({
                                position: { x: 130, y: 10 }, size: { width: 60, height: 60 },
                                roger_federation: {
                                    type: "Data Store"
                                },
                                icon: "dataStore"
                            })
                        ], 'data');
                        addStencilToolip(stencil.graphs.data);
                        //Artifacts group
                        stencil.load([
                            new joint.shapes.bpmn.Group({
                                position: { x: 10, y: 35 }, size: { width: 80, height: 70 },
                                roger_federation: {
                                    name: "",
                                    type: "Group"
                                },
                                attrs: {
                                    '.': {
                                        magnet: false
                                    },
                                    '.label': {
                                        text: 'Group'
                                    }
                                }
                            }),
                            new joint.shapes.bpmn.Message({
                                position: { x: 130, y: 10 }, size: { width: 60, height: 30 },
                                roger_federation: {
                                    type: "Message"
                                }
                            }),
                            new joint.shapes.bpmn.Annotation({
                                position: { x: 130, y: 70 }, size: { width: 80, height: 30 },
                                roger_federation: {
                                    type: "Annotation"
                                },
                                content: "Enter text..."
                            })
                        ], 'artifacts');
                        addStencilToolip(stencil.graphs.artifacts);
                    } else if (diagramType === "Federation") {
                        stencil.load([
                            new joint.shapes.bpmn.Activity({
                                position: { x: 10, y: 10 }, size: { width: 80, height: 80 },
                                roger_federation: {
                                    name: "",
                                    type: "Federate",
                                    stringId: "",
                                    description: "",
                                    attributes: [],
                                    groups: []
                                },
                                graphType: "FederateCell",
                                icon: "federation"
                            }),
                            new joint.shapes.bpmn.Activity({
                                position: { x: 100, y: 10 }, size: { width: 80, height: 80 },
                                roger_federation: {
                                    name: "",
                                    type: "Group",
                                    stringId: "",
                                    description: "",
                                    interconnected: false,
                                    groupFilters: [],
                                    attributes: []
                                },
                                graphType: "GroupCell",
                                icon: "circle"
                            }),
                            new joint.shapes.bpmn.Activity({
                                position: { x: 10, y: 100 }, size: { width: 80, height: 80 },
                                roger_federation: {
                                    name: "",
                                    type: "FederationOutgoing",
                                    stringId: "",
                                    description: "",
                                    interconnected: false,
                                    groupFilters: [],
                                    attributes: []
                                },
                                graphType: "FederationOutgoingCell",
                                attrs: {
                                    '.body': { fill: 'gray', stroke: 'black', opacity: '0.40' },
                                },
                                icon: "outgoing"
                            })
                        ], 'participants');
                        addStencilToolip(stencil.graphs.participants);
                    }
                    /* beautify preserve:end */
                },
                getPaper: function() {
                    return paper;
                },
                inspector: undefined,
                options: options,
                cleanUpInlineEditor: cleanUpInlineEditor,
                autosizeCellView: autosizeCellView,
                openInlineEditor: openInlineEditor
            };
        }
    ]);
