var inputs = {

    'bpmn.Gateway': {
        icon: {
            type: 'select',
            options: [
                { value: 'none', content: 'default' },
                { value: 'cross', content: 'exclusive' },
                { value: 'circle', content: 'inclusive' },
                { value: 'plus', content: 'parallel' }
            ],
            label: 'Type',
            group: 'general',
            index: 2
        },
        attrs: {
            '.label/text': {
                type: 'text',
                label: 'Name',
                group: 'general',
                index: 1
            },
            // '.body/fill': {
            //     type: 'color',
            //     label: 'Body Color ',
            //     group: 'appearance',
            //     index: 1
            // }
        }
    },

    'bpmn.Activity': {
        // content: {
        //     type: 'textarea',
        //     label: 'Content',
        //     group: 'general',
        //     index: 1
        // },
        // customRegion: {
        //   type: 'text',
        //   label: 'Role / Product',
        //   group: 'general',
        //   index: 1
        // },
        // content: {
        //     type: 'textarea',
        //     label: 'Role / Product',
        //     group: 'general',
        //     index: 1
        // },
        icon: {
            type: 'select',
            options: ['none','message','user'],
            label: 'Icon',
            group: 'general',
            index: 2
        },
        activityType: {
            type: 'select',
            options: ['task', 'transaction', 'event-sub-process', 'call-activity'],
            label: 'Type',
            group: 'general',
            index: 3
        },
        subProcess: {
            type: 'toggle',
            label: 'Sub-process',
            group: 'general',
            index: 4
        },
        // attrs: {
        //     '.body/fill': {
        //         type: 'color',
        //         label: 'Body Color',
        //         group: 'appearance',
        //         index: 1
        //     }
        // }
    },

    'bpmn.Event': {
        eventType: {
            type: 'select',
            options: ['start','end','intermediate'],
            group: 'general',
            label: 'Type',
            index: 2
        },
        icon: {
            type: 'select',
            options: [
                { value: 'none', content: 'none' },
                { value: 'cross', content: 'cancel' },
                { value: 'message', content: 'message' },
                { value: 'plus', content: 'parallel multiple' }
            ],
            label: 'Subtype',
            group: 'general',
            index: 3
        },
        attrs: {
            '.label/text': {
                type: 'text',
                label: 'Name',
                group: 'general',
                index: 1
            },
            // '.body/fill': {
            //     type: 'color',
            //     label: 'Body Color',
            //     group: 'appearance',
            //     index: 1
            // }
        }
    },

    'bpmn.Annotation': {
        content: {
            type: 'textarea',
            label: 'Content',
            group: 'general',
            index: 1
        },
        attrs: {
            // '.body/fill': {
            //     type: 'color',
            //     label: 'Body Color',
            //     group: 'appearance',
            //     index: 1
            // },
            // '.body/fill-opacity': {
            //     type: 'range',
            //     min: 0,
            //     max: 1,
            //     step: 0.1,
            //     label: 'Transparency',
            //     group: 'appearance',
            //     index: 2
            // }

        }
    },

    'bpmn.Pool': {
        lanes: {
            type: 'object',
            group: 'general',
            index: 1,
            attrs: {
                label: {
                    style: 'display:none;'
                }
            },
            properties: {
                label: {
                    type: 'text',
                    label: 'Label'
                },
                sublanes: {
                    type: 'list',
                    label: 'Add lanes',
                    item: {
                        type: 'object',
                        properties: {
                            label: {
                                type: 'text',
                                label: 'Label',
                                attrs: {
                                    label: {
                                        style: 'display:none;'
                                    }
                                }
                            },
                            sublanes: {
                                type: 'list',
                                label: 'Add sublanes',
                                item: {
                                    type: 'object',
                                    properties: {
                                        label: {
                                            type: 'text',
                                            label: 'Label',
                                            attrs: {
                                                label: {
                                                    style: 'display:none;'
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        attrs: {
            // '.body/fill': {
            //     type: 'color',
            //     label: 'Body Color',
            //     group: 'appearance',
            //     index: 1
            // },
            // '.header/fill': {
            //     type: 'color',
            //     label: 'Header Color',
            //     group: 'appearance',
            //     index: 2
            // },
            // '.lane-body/fill': {
            //     type: 'color',
            //     label: 'Lane Body Color',
            //     group: 'appearance',
            //     index: 3
            // },
            // '.lane-header/fill': {
            //     type: 'color',
            //     label: 'Lane Header Color',
            //     group: 'appearance',
            //     index: 4
            // }
        }
    },

    'bpmn.Group': {
        attrs: {
            '.label/text': {
                type: 'text',
                label: 'Name',
                group: 'general',
                index: 1
            },
            // '.label-rect/fill': {
            //     type: 'color',
            //     label: 'Body Color',
            //     group: 'appearance',
            //     index: 1
            // }
        }
    },

    'bpmn.Conversation': {
        conversationType: {
            type: 'select',
            options: ['conversation', 'call-conversation'],
            label: 'Type',
            group: 'general',
            index: 2
        },
        subProcess: {
            type: 'toggle',
            label: 'Sub-process',
            group: 'general',
            index: 3
        },
        attrs: {
            '.label/text': {
                type: 'text',
                label: 'Name',
                group: 'general',
                index: 1
            },
            // '.body/fill': {
            //     type: 'color',
            //     label: 'Body Color',
            //     group: 'appearance',
            //     index: 1
            // }
        }
    },

    'bpmn.Choreography': {
        participants: {
            type: 'list',
            label: 'Particpants',
            item: {
                type: 'text'
            },
            group: 'general',
            index: 1
        },
        initiatingParticipant: {
            type: 'select',
            label: 'Initiating Participant',
            options: 'participants',
            group: 'general',
            index: 2
        },
        subProcess: {
            type: 'toggle',
            label: 'Sub-process',
            group: 'general',
            index: 3
        },
        content: {
            type: 'textarea',
            label: 'Content',
            group: 'general',
            index: 4
        },
        attrs: {
            // '.body/fill': {
            //     type: 'color',
            //     label: 'Primary Color',
            //     group: 'appearance',
            //     index: 1
            // },
            // '.participant-rect/fill': {
            //     type: 'color',
            //     label: 'Secondary Color',
            //     group: 'appearance',
            //     index: 2
            // }
        }
    },

    'bpmn.DataObject': {
        attrs: {
            '.label/text': {
                type: 'text',
                label: 'Name',
                group: 'general',
                index: 1
            },
            // '.body/fill': {
            //     type: 'color',
            //     label: 'Body Color',
            //     group: 'appearance',
            //     index: 1
            // }
        }
    },

    'bpmn.Message': {
        attrs: {
            '.label/text': {
                type: 'text',
                label: 'Name',
                group: 'general',
                index: 1
            },
            // '.body/fill': {
            //     type: 'color',
            //     label: 'Body Color',
            //     group: 'appearance',
            //     index: 1
            // }
        }
    },

    'bpmn.Flow': {
      labels: [
          [{
            attrs:{
              text: {
                  type: 'text',
                  label: 'text',
                  group: 'general',
                  index: 1
                }
              }
            }]
          ],

        flowType: {
            type: 'select',
            options: ['default', 'conditional','normal','message','association','conversation'],
            label: 'Type',
            group: 'general',
            index: 1
        }
    }
};
