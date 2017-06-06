/*
 * Copyright 2016 Aitu Software Limited.
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
    function drawTimeSeries(percentileData, boundMetric) {
        var metricNormalisedName = boundMetric.getNormalisedName();
        var metricThresholds = boundMetric.getMetricThresholds();

        var canvas = d3.select('#percentile_chart_' + metricNormalisedName).attr('width', '800').attr('height', '300');
        var context = canvas.node().getContext('2d');
        var margin = {top: 20, right: 20, bottom: 30, left: 50},
            width = canvas.node().width - margin.left - margin.right,
            height = canvas.node().height - margin.top - margin.bottom;

        context.translate(margin.left, margin.top);

        var xScale = d3.scaleLinear().range([0, width]);
        var yScale = d3.scaleLinear().range([height, 0]);

        var startTimestamp = Number.MAX_VALUE;
        var endTimestamp = 0;
        var maxValue = 0;
        var minValue = Number.MAX_VALUE;
        for(var i = 0; i < percentileData.length; i++) {
            var percentiles = percentileData[i];
            for(var j = 0; j < percentiles.data.length; j++) {
                maxValue = Math.max(maxValue, percentiles.data[j].max);
                minValue = Math.min(minValue, percentiles.data[j].min);
                startTimestamp = Math.min(startTimestamp, percentiles.data[j].start);
                endTimestamp = Math.max(startTimestamp, percentiles.data[j].end);
            }
        }

        xScale.domain([startTimestamp, endTimestamp]);
        yScale.domain([minValue, maxValue]);


        var horizontalTickSize = 6;

        var hTickCount = 3 + Math.floor((endTimestamp - startTimestamp) / 60000);
        var horizontalTicks = xScale.ticks(hTickCount);
        var hTickFormat = xScale.tickFormat(hTickCount);

        context.beginPath();
        horizontalTicks.forEach(function(d) {
            context.moveTo(xScale(d), height);
            context.lineTo(xScale(d), height + horizontalTickSize);
        });
        context.strokeStyle = "black";
        context.stroke();

        context.textAlign = "center";
        context.textBaseline = "top";
        var hFormat = d3.timeFormat("%H:%M:%S");
        horizontalTicks.forEach(function(d) {
            context.fillText(hFormat(new Date(d)), xScale(d), height + horizontalTickSize);
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

        for(var i = 0; i < percentileData.length; i++) {
            var percentiles = percentileData[i];

            var maxLine = d3.line().x(function(d) {
                return xScale(d.start);
            }).y(function(d) { return yScale(d.max); }).
            context(context);

            context.beginPath();
            maxLine(percentiles.data);
            context.lineWidth = 1.5;
            context.strokeStyle = d3.schemeCategory10[0];
            context.stroke();

            var minLine = d3.line().x(function(d) {
                return xScale(d.start);
            }).y(function(d) { return yScale(d.min); }).
            context(context);

            context.beginPath();
            minLine(percentiles.data);
            context.lineWidth = 1.5;
            context.strokeStyle = d3.schemeCategory10[1];
            context.stroke();

            var meanLine = d3.line().x(function(d) {
                return xScale(d.start);
            }).y(function(d) { return yScale(d.mean); }).
            context(context);

            context.beginPath();
            meanLine(percentiles.data);
            context.lineWidth = 1.5;
            context.strokeStyle = d3.schemeCategory10[2];
            context.stroke();
        }
    };

    function getDataUrl(metric, reportWindow) {
        return '../../query/timeSeries/' + metric.metricName + '/5/MINUTES';
    }

    this.chartFunctions = {
        draw: drawTimeSeries,
        getDataUrl: getDataUrl
    };
}(this));
