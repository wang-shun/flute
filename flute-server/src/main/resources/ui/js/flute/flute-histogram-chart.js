/*
 * Copyright 2016 - 2017 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function (global) {
    function drawHistogram(percentileData, boundMetric) {
        var metricNormalisedName = boundMetric.getNormalisedName();
        var metricThresholds = boundMetric.getMetricThresholds();

        var canvas = d3.select('#percentile_chart_' + metricNormalisedName).attr('width', '400').attr('height', '300');
        var context = canvas.node().getContext('2d');
        var margin = {top: 20, right: 20, bottom: 30, left: 50},
            width = canvas.node().width - margin.left - margin.right,
            height = canvas.node().height - margin.top - margin.bottom;

        context.translate(margin.left, margin.top);

        var xScale = d3.scaleLog().range([0, width]);
        var yScale = d3.scaleLinear().range([height, 0]);

        var maxRatio = 0;
        var maxValue = 0;
        var minRatio = Number.MAX_VALUE, minValue = Number.MAX_VALUE;
        for(var i = 0; i < percentileData.length; i++) {
            var percentiles = percentileData[i];
            for(var j = 0; j < percentiles.data.length; j++) {
                maxRatio = Math.max(maxRatio, percentiles.data[j].ratio);
                maxValue = Math.max(maxValue, percentiles.data[j].value);
                minRatio = Math.min(minRatio, percentiles.data[j].ratio);
                minValue = Math.min(minValue, percentiles.data[j].value);
            }
        }

        xScale.domain([minRatio, maxRatio]);
        yScale.domain([minValue, maxValue]);


        var horizontalTickSize = 6;
        var horizontalTicks = [1, 10, 100, 1000, 10000, 100000, 1000000, 10000000];
        var percentileLookup = [];
        percentileLookup[1] = '0%';
        percentileLookup[10] = '90%';
        percentileLookup[100] = '99%';
        percentileLookup[1000] = '99.9%';
        percentileLookup[10000] = '99.99%';
        percentileLookup[100000] = '99.999%';
        percentileLookup[1000000] = '99.9999%';
        percentileLookup[10000000] = '99.99999%';
        percentileLookup[100000000] = '99.999999%';

        context.beginPath();
        horizontalTicks.forEach(function(d) {
            context.moveTo(xScale(d), height);
            context.lineTo(xScale(d), height + horizontalTickSize);
        });
        context.strokeStyle = "black";
        context.stroke();

        context.textAlign = "center";
        context.textBaseline = "top";
        horizontalTicks.forEach(function(d) {
            context.fillText(percentileLookup[d], xScale(d), height + horizontalTickSize);
        });

        var verticalTickCount = 10;
        var verticalTickSize = 6;
        var tickPadding = 3;
        var verticalTicks = yScale.ticks(verticalTickCount);
        var tickFormat = yScale.tickFormat(verticalTickCount);

        context.beginPath();
        verticalTicks.forEach(function(d) {
            context.moveTo(0, yScale(d));
            context.lineTo(-6, yScale(d));
        });
        context.strokeStyle = "black";
        context.stroke();

        context.beginPath();
        context.moveTo(-verticalTickSize, 0);
        context.lineTo(0.5, 0);
        context.lineTo(0.5, height);
        context.lineTo(-verticalTickSize, height);
        context.strokeStyle = "black";
        context.stroke();

        context.textAlign = "right";
        context.textBaseline = "middle";
        verticalTicks.forEach(function(d) {
            context.fillText(d, -verticalTickSize - tickPadding, yScale(d));
        });

        context.save();
        context.rotate(-Math.PI / 2);
        context.textAlign = "right";
        context.textBaseline = "top";
        context.font = "bold 10px sans-serif";
        context.fillText("Latency (us)", -10, 10);
        context.restore();

        percentileData.sort(function(a, b) {
            return a.index - b.index;
        });
        // console.log(metricNormalisedName + ' line count: ' + percentileData.length);
        for(var i = 0; i < percentileData.length; i++) {
            var percentiles = percentileData[i];
            var lineDebug = '';
            var line = d3.line().x(function(d) {
                lineDebug += '(x:' + d.ratio;
                return xScale(d.ratio);
            }).y(function(d) {
                lineDebug += ',y:' + d.value + '),';
                return yScale(d.value);
            }).
            context(context);

            context.beginPath();
            line(percentiles.data);
            // console.log(percentiles.reportWindow + ': ' + lineDebug);
            context.lineWidth = 1.5;
            context.strokeStyle = d3.schemeCategory10[percentiles.index];
            context.stroke();
        }

        var slaPoints = [];
        metricThresholds.forEach(function(m) {
            if(m.name === 'NINETIETH') {
                slaPoints.push({x: 10, value: m.value});
            } else if(m.name === 'TWO_NINES') {
                slaPoints.push({x: 100, value: m.value});
            } else if(m.name === 'THREE_NINES') {
                slaPoints.push({x: 1000, value: m.value});
            } else if(m.name === 'FOUR_NINES') {
                slaPoints.push({x: 10000, value: m.value});
            } else if(m.name === 'FIVE_NINES') {
                slaPoints.push({x: 100000, value: m.value});
            } else if(m.name === 'SIX_NINES') {
                slaPoints.push({x: 1000000, value: m.value});
            } else if(m.name === 'MIN') {
                slaPoints.push({x: 1, value: m.value});
            } else if(m.name === 'MAX') {
                slaPoints.push({x: maxRatio, value: m.value});
            }
        });

        slaPoints.sort(function(a, b) {
            return a.x - b.x;
        });
        var slaIndicator = [];
        var lastX = 1;
        var lastY;

        slaPoints.forEach(function(d) {
            slaIndicator.push({x: lastX, value: d.value});
            lastX = d.x;
            slaIndicator.push({x: d.x, value: d.value});
        });

        var slaLine = d3.line().
            x(function(d) {
                return xScale(d.x);
            }).y(function(d) {
                return yScale(Math.min(d.value, maxValue));
            }).
            context(context);

        context.beginPath();
        slaLine(slaIndicator);
        context.lineWidth = 1.5;
        context.strokeStyle = '#333';
        context.stroke();
    };

    function getDataUrl(metric, reportWindow, endTimestamp) {
        return '/flute/app/query/slaPercentiles/' +
             metric.metricName + '/' +
             reportWindow.duration + '/' +
             reportWindow.unit;
    };

    this.chartFunctions = {
        draw: drawHistogram,
        getDataUrl: getDataUrl
    };
}(this));
