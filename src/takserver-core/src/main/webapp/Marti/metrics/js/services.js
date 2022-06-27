var services = angular.module('metricsManagerServices', []);
services.service('MetricsService', function($resource, $http) {
    this.getCustomMemoryMetrics = function() {
        return $resource('/actuator/custom-memory-metrics');
    };
    this.getCustomNetworkMetrics = function() {
        return $resource('/actuator/custom-network-metrics');
    };
    this.getCustomQueueMetrics = function() {
        return $resource('/actuator/custom-queue-metrics');
    };
    this.getConnectionCustomNetworkMetrics = function(uid) {
        return $http({
            url: '/actuator/custom-network-metrics/' + uid,
            method: "POST",
            data: {uid}
        })
    };
    this.getCustomCpuMetrics = function() {
        return $resource('/actuator/custom-cpu-metrics');
    };
    this.getStartTime = function() {
        return $resource('/actuator/metrics/process.start.time');
    };
    this.getDatabaseMetrics = function() {
        return $resource('/actuator/takserver-database');
    };
});

services.factory('CpuServices', function($resource) {
    return $resource('/actuator/metrics/system.cpu.usage', {}, {
        'query': {
            method: "GET",
            isArray: false
        }
    })
});

services.factory('DonutService', function() {
    return function() {
        var chartObj = this;
        var color = ['green', 'yellow'] 
        var arc;       
        this.height = 0;
        this.width = 0;
        this.selector = '';

        this.setWidth = function(width) {
            this.width = width;
            return this;
        }
        this.setHeight = function(height) {
            this.height = height;
            return this;
        }
        this.setSelector = function(selector) {
            this.selector = selector;
            return this;
        }


        this.init = function(){
            var svg = d3.select(this.selector)
                .append("svg")
                .attr("width", (this.width / $(this.selector).css('width').split("px")[0]) * 100 + '%')
                .attr("class", "cpusvg")
                .attr("position", "absolute")
                .attr("preserveAspectRatio", "xMinYMin meet")
                .attr("viewBox", "0 0 " + this.width + " " + this.height)
                .classed("svg-content", true);

            // set the thickness of the inner and outer radii
            this.height = $(this.selector).find('.cpusvg').css('width').split("px")[0]
            this.width = $(this.selector).find('.cpusvg').css('width').split("px")[0]

            var min = Math.min(this.width, this.height);
            var oRadius = min / 2 * 0.9;
            var iRadius = min / 2 * 0.7;
            // construct default pie laoyut
            var pie = d3.pie().value(function(d) {
                return d;
            }).sort(null);
            this.pie = pie;
            // construct arc generator
            arc = d3.arc()
                .outerRadius(oRadius)
                .innerRadius(iRadius)
                .padAngle(0.03);
            // creates the pie chart container
            let height = this.height;
            let width = this.height;
            var g = svg.append('g')
                .attr("margin", "0 auto")
                .attr('transform', function() {
                    return 'translate(' + width / 2 + ',' + height / 2 + ')';
                });
            this.g = g;
            // generate random data
            var data = [0, 100];

            var text = g.append('text')
                .attr('dominant-baseline', 'middle')
                .attr('text-anchor', 'middle')
                .attr("font-size", this.width * .15 + "px")
                .text(Math.round((data[0] * 10) / 10) + '%');
            this.text = text;
            // enter data and draw pie chart
            var path = g.datum(data).selectAll("path")
                .data(pie)
                .enter()
                .append("path")
                .attr("fill", function(d, i) {
                    return color[i];
                })
                .attr("d", arc)
                .each(function(d) {
                    this._current = d;
                })
        }

        this.redraw = function(rounded) {
            let arr = [rounded, 100 - rounded];
            // add transition to new path
            this.text
                .text((rounded) + '%')

            this.g.datum(arr).selectAll("path").attr("fill", function(d, i) {
                return i == 1 ? 'green' : getGradient(rounded);
            });

            this.g.datum(arr).selectAll("path")
                .data(this.pie)
                .transition()
                .duration(1000)
                .attrTween("d", arcTween)
        }

        // Store the displayed angles in _current.
        // Then, interpolate from _current to the new angles.
        // During the transition, _current is updated in-place by d3.interpolate.
        function arcTween(a) {
            var i = d3.interpolate(this._current, a);
            this._current = i(0);
            return function(t) {
                return arc(i(t));
            };
        }

        function getGradient(val) {
            if (val < 15) return '#E59700';
            else if (val < 30) return '#E58A00';
            else if (val < 45) return '#E57E00';
            else if (val < 60) return '#E57100';
            else if (val < 75) return '#E55800';
            else if (val < 90) return '#E53F00';
            else return '#E53200';
        }
    }
});

services.factory('ChartService', function() {
    return function() {
        var chartObj = this;
        this.height = 0;
        this.width = 0;
        this.selector = '';
        this.numLines = 0;
        this.areaUnderLine = false;
        this.lineColors = []
        this.areaColors = []
        this.areaOpacity = []
        this.maxDataPoints = 30;
        this.margins = {
            right: 0,
            left: 0,
            top: 0,
            bottom: 0
        };

        this.setWidth = function(width) {
            this.width = width;
            return this;
        }
        this.setHeight = function(height) {
            this.height = height;
            return this;
        }
        this.setSelector = function(selector) {
            this.selector = selector;
            return this;
        }
        this.setNumLines = function(numLines) {
            this.numLines = numLines;
            return this;
        }
        this.setAreaUnderLine = function(areaUnderLine) {
            this.areaUnderLine = areaUnderLine;
            return this;
        }
        this.setLineColors = function(lineColors) {
            this.lineColors = lineColors;
            return this;
        }
        this.setAreaColors = function(areaColors) {
            this.areaColors = areaColors;
            return this;
        }
        this.setAreaOpacity = function(areaOpacity) {
            this.areaOpacity = areaOpacity;
            return this;
        }
        this.setMaxDataPoints = function(maxDataPoints) {
            this.maxDataPoints = maxDataPoints;
            return this;
        }
        this.setMargins = function(margins) {
            this.margins = margins;
            return this;
        }

        var lines = [];
        var areas = [];
        var lineElms = [];
        var areaElms = [];
        var dataArrays = [];

        this.init = function() {
            this.height = this.height - this.margins.top - this.margins.bottom;
            this.width = this.width - this.margins.right - this.margins.left;

            this.xScale = d3.scaleTime();
            this.yScale = d3.scaleLinear();
            this.xAxis = d3.axisBottom(this.xScale)
            this.yAxis = d3.axisLeft(this.yScale).tickFormat(d3.format(".0s"));

            var svg = d3.select(this.selector)
                .append('svg')
                .attr('width', this.width + this.margins.left + this.margins.right)
                .attr('height', this.height + this.margins.top + this.margins.bottom)
                .append('g')
                .attr('transform', 'translate(' + this.margins.left + ',' + this.margins.top + ")")

            svg.append("g")
                .attr("class", "x grid")
                .attr("transform", "translate(0," + this.height + ")")
                .call(d3.axisBottom(this.xScale))

            // add the Y gridlines
            svg.append("g")
                .attr("class", "y grid")
                .call(d3.axisLeft(this.yScale))

            svg.append('g')
                .attr('class', 'x axis')
                .attr('transform', 'translate(0,' + this.height + ')')
                .call(this.xAxis)

            svg.append('g')
                .attr('class', 'y axis')
                .call(this.yAxis)

            for (let i = 0; i < this.numLines; i++) {
                let arr = [];

                let line = d3.line()
                    .x(function(d) {
                        return chartObj.xScale(d.date);
                    })
                    .y(function(d) {
                        return chartObj.yScale(d.value);
                    });

                if (this.areaUnderLine) {
                    let area = d3.area()
                        .x(function(d) {
                            return chartObj.xScale(d.date);
                        })
                        .y0(this.height)
                        .y1(function(d) {
                            return chartObj.yScale(d.value);
                        });

                    areaElm = svg.append("path")
                        .datum(arr)
                        .style("fill", this.areaColors[i])
                        .style('opacity', this.areaOpacity[i])
                        .attr("d", area);
                    areas.push(area);
                    areaElms.push(areaElm);
                }

                lineElm = svg.append('path')
                    .datum(arr)
                    .style("stroke", this.lineColors[i])
                    .style("stroke-width", '2px')
                    .style("fill", 'none')
                    .attr('d', line);

                lines.push(line);
                lineElms.push(lineElm);
                dataArrays.push(arr);
            }
        }

        this.clear = function() {
            for (let i = 0; i < this.numLines; i++) {
                dataArrays[i].length = 0;
            }
        }

        this.addDataPoint = function(newData) {
            let date = new Date()
            for (let i = 0; i < dataArrays.length; i++) {
                if (this.maxDataPoints < dataArrays[i].length) {
                    dataArrays[i].shift()
                }
                dataArrays[i].push({
                    date,
                    value: newData[i] ? newData[i] : 0
                })
            }
        }
        this.redraw = function(newData) {
            this.xScale.domain(d3.extent(dataArrays.flatMap(x => x), function(d) {
                    return d.date
                }))
                .range([0, this.width])
            let yRange = [0, d3.max(dataArrays.flatMap(x => x), function(d) {
                return d.value * 1.1
            })];
            this.yScale.domain(yRange)
                .range([this.height, 0])

            for (let i = 0; i < dataArrays.length; i++) {
                lineElms[i].attr('d', lines[i])
                if (this.areaUnderLine) areaElms[i].attr('d', areas[i])
            }
            this.yAxis.tickFormat(yRange[1] > 999 ? d3.format(".0s") : d3.format(""));
            d3.select(this.selector).select('.x.axis').transition().call(this.xAxis)
            d3.select(this.selector).select('.y.axis').transition().call(this.yAxis)
            d3.select(this.selector).select('.x.grid').call(d3.axisBottom(this.xScale)
                .tickSize(-this.height)
                .tickFormat(""))
            d3.select(this.selector).select('.y.grid').call(d3.axisLeft(this.yScale)
                .tickSize(-this.width)
                .tickFormat(""))
        }
    }
});