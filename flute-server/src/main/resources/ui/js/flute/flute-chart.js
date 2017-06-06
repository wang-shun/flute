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
var METRIC_NAME_TO_VALUE_INDEX = {};
METRIC_NAME_TO_VALUE_INDEX["TIMESTAMP"] = 0;
METRIC_NAME_TO_VALUE_INDEX["MIN"] = 1;
METRIC_NAME_TO_VALUE_INDEX["MEAN"] = 2;
METRIC_NAME_TO_VALUE_INDEX["FIFTIETH"] = 3;
METRIC_NAME_TO_VALUE_INDEX["NINETIETH"] = 4;
METRIC_NAME_TO_VALUE_INDEX["TWO_NINES"] = 5;
METRIC_NAME_TO_VALUE_INDEX["THREE_NINES"] = 6;
METRIC_NAME_TO_VALUE_INDEX["FOUR_NINES"] = 7;
METRIC_NAME_TO_VALUE_INDEX["FIVE_NINES"] = 8;
METRIC_NAME_TO_VALUE_INDEX["MAX"] = 9;
METRIC_NAME_TO_VALUE_INDEX["COUNT"] = 10;

var METRIC_NAME_TO_DISPLAY_NAME = {};
METRIC_NAME_TO_DISPLAY_NAME["MIN"] = 'Min';
METRIC_NAME_TO_DISPLAY_NAME["MEAN"] = 'Mean';
METRIC_NAME_TO_DISPLAY_NAME["FIFTIETH"] = '50th';
METRIC_NAME_TO_DISPLAY_NAME["NINETIETH"] = '90th';
METRIC_NAME_TO_DISPLAY_NAME["TWO_NINES"] = '99th';
METRIC_NAME_TO_DISPLAY_NAME["THREE_NINES"] = '99.9th';
METRIC_NAME_TO_DISPLAY_NAME["FOUR_NINES"] = '99.99th';
METRIC_NAME_TO_DISPLAY_NAME["FIVE_NINES"] = '99.999th';
METRIC_NAME_TO_DISPLAY_NAME["MAX"] = 'Max';
METRIC_NAME_TO_DISPLAY_NAME["COUNT"] = 'Count';

(function outer_init() {
    var reportMode = false;
    var reportName = null;
    var currentReportConfig = null;
    var displayPercentileCharts = true;

    function flute_init() {
        d3.selectAll('.status').text('No report specified.');

        var reportNameMatch = /.*(\?|&)report\=([^&]+)/.exec(window.location.search);
        if(reportNameMatch !== null) {
            reportName = reportNameMatch[2];
            fluteUtil.get('../../report/spec/' + reportName,
                function(reportConfig) {
                    d3.selectAll('.status').text('Retrieved reporting config.');
                    d3.select('#headerReportName').text(reportName);
                    currentReportConfig = reportConfig;
                    createApp(reportConfig);
                    setInterval(reloadReportConfig, 5000);
                },
                function(statusCode) {
                    d3.selectAll('.status').text('Failed to retrieve config for report ' + reportName + ' code was ' + statusCode);
                }
            );
            reportMode = true;
        }
    }

    function reloadReportConfig() {
        fluteUtil.get('../../report/spec/' + reportName,
            function(reportConfig) {
                if(reportConfig[0].metricThresholds.length !== currentReportConfig[0].metricThresholds.length) {
                    window.location.reload();
                }
            },
            function(statusCode) {
                d3.selectAll('.status').text('Failed to retrieve config for report ' + reportName + ' code was ' + statusCode);
            }
        );
    }

    function createApp(reportingConfig) {
        var fluteApplication = configureApplication(reportingConfig);
        var view = createView();
        view.layout(fluteApplication.getMetrics(),
                    getLayoutProperties(fluteApplication.getMetrics(), fluteApplication.getReportWindows()),
                    fluteApplication.getReportWindows());
        fluteApplication.poll(view);
        setInterval(function() {
            fluteApplication.poll(view);
        }, 5000);
    }

    function getLayoutProperties(metrics, reportWindows) {
        var maxThresholdCountInAnyMetric = 0;
        for(var i = 0; i < metrics.length; i++) {
            var metricThresholdCount = metrics[i].getThresholdNames().length;
            maxThresholdCountInAnyMetric = Math.max(metricThresholdCount, maxThresholdCountInAnyMetric);
        }

        return {
            columnCount: maxThresholdCountInAnyMetric + 1,
            rowCount: (metrics.length + 2) * reportWindows.length
        }
    }

    function configureApplication(reportingConfig) {
        return {
            metricThresholds: reportingConfig[0].metricThresholds,
            reportWindows: reportingConfig[0].reportWindows,
            metrics: [],

            getMetrics: function() {
                if(this.metrics.length === 0) {
                    for(var i = 0; i < this.metricThresholds.length; i++) {
                        this.metrics.push(toMetric(this.metricThresholds[i]));
                    }
                }

                return this.metrics;
            },

            getReportWindows: function() {
                return this.reportWindows;
            },

            poll: function(view) {
                var currentTime = new Date().getTime();
                var globalCounters = {};
                for(var m = 0; m < this.getMetrics().length; m++) {
                    var metric = this.getMetrics()[m];
                    globalCounters[metric.getNormalisedName()] = {};
                    globalCounters[metric.getNormalisedName()]['thresholdExceededInAnyReportWindow'] = false;
                    globalCounters[metric.getNormalisedName()]['remainingReportWindowCount'] = this.reportWindows.length;
                    globalCounters[metric.getNormalisedName()]['remainingPercentileDataCount'] = this.reportWindows.length;
                    globalCounters[metric.getNormalisedName()]['percentileData'] = [];

                    for(var r = 0; r < this.reportWindows.length; r++) {
                        var reportWindow = this.reportWindows[r];
                        var url = '../../query/slaReport/' +
                                    metric.metricName + '/' +
                                    reportWindow.duration + '/' +
                                    reportWindow.unit;

                        var percentileUrl = '../../query/timeSeries/' +
                                    metric.metricName + '/5/MINUTES';

                        (function (boundMetric, reportWindow, url, index) {
                            d3.json(url, function(data) {

//                                console.log(data);

                                globalCounters[boundMetric.getNormalisedName()]['remainingPercentileDataCount']--;
                                var percentileData = globalCounters[boundMetric.getNormalisedName()]['percentileData'];
                                if(data && data.length) {
                                    percentileData.push({reportWindow: reportWindow.duration + '_' + reportWindow.unit, data: data, index: index});
                                }

                                if(globalCounters[boundMetric.getNormalisedName()]['remainingPercentileDataCount'] === 0) {
                                    drawPercentileChart(percentileData, boundMetric);
                                }
                            });
                        })(metric, reportWindow, percentileUrl, r);

                        (function (boundMetric, reportWindow, url) {
                            d3.json(url, function(data) {
                                if(data && data.length !== 0) {
                                    var dataSet = data[data.length - 1];
                                    var allBelowThreshold = true;
                                    // TODO decide whether to collapse rows
                                    allBelowThreshold = false;
                                    for(var i = 0; i < boundMetric.getMetricThresholds().length; i++) {
                                        var threshold = boundMetric.getMetricThresholds()[i];
                                        var value = parseInt(dataSet[METRIC_NAME_TO_VALUE_INDEX[threshold.name]]);
                                        if(value >= boundMetric.getThresholdValue(threshold.name)) {
                                            allBelowThreshold = false;
                                        }
                                    }
                                    var normalisedName = boundMetric.getNormalisedName();
                                    if(!allBelowThreshold) {
                                        globalCounters[normalisedName]['thresholdExceededInAnyReportWindow'] = true;
                                    }
                                    globalCounters[normalisedName]['remainingReportWindowCount']--;
                                    if(globalCounters[normalisedName]['remainingReportWindowCount'] === 0) {
                                        if(!globalCounters[normalisedName]['thresholdExceededInAnyReportWindow']) {
                                            d3.selectAll('.row_' + normalisedName).
                                                selectAll('td').text('');
                                        } else {
                                            for(var j = 0; j < boundMetric.getMetricThresholds().length; j++) {
                                                var threshold = boundMetric.getMetricThresholds()[j];
                                                var selector = '.threshold_' + normalisedName + '_' + threshold.name;
                                                d3.selectAll(selector).text(METRIC_NAME_TO_DISPLAY_NAME[threshold.name]);
                                            }
                                        }
                                    }

                                    var reportWindowName = reportWindow.duration + '_' + reportWindow.unit;

                                    var recordCount = dataSet[METRIC_NAME_TO_VALUE_INDEX['COUNT']];

                                    if(recordCount === 0) {
                                        indicateNoDataForCell(normalisedName, 'COUNT', reportWindowName);
                                        for(var i = 0; i < boundMetric.getMetricThresholds().length; i++) {
                                            var threshold = boundMetric.getMetricThresholds()[i];
                                            indicateNoDataForCell(normalisedName, threshold.name, reportWindowName);
                                        }
                                    } else {
                                        _updateCell(normalisedName, 'COUNT',
                                                reportWindowName,
                                                recordCount, Number.MAX_VALUE, !allBelowThreshold);
                                        for(var i = 0; i < boundMetric.getMetricThresholds().length; i++) {
                                            var threshold = boundMetric.getMetricThresholds()[i];
                                            var value = parseInt(dataSet[METRIC_NAME_TO_VALUE_INDEX[threshold.name]]);
                                            _updateCell(normalisedName, threshold.name,
                                                reportWindowName, value,
                                                boundMetric.getThresholdValue(threshold.name), !allBelowThreshold);
                                        }
                                        if(allBelowThreshold) {
                                            d3.selectAll('.label_' + normalisedName + '_' + reportWindowName).
                                                text('');
                                        } else {
                                            d3.selectAll('.label_' + normalisedName + '_' + reportWindowName).
                                                text(windowDurationToDisplayText(reportWindow));
                                        }
                                    }
                                }
                            });
                        })(metric, reportWindow, url);
                    }
                }
            }

        }
    }

    function drawPercentileChart(percentileData, boundMetric) {
        var metricNormalisedName = boundMetric.getNormalisedName();
        var metricThresholds = boundMetric.getMetricThresholds();

        var canvas = d3.select('#percentile_chart_' + metricNormalisedName).attr('width', '400').attr('height', '300');
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

        var hTickCount = Math.floor((endTimestamp - startTimestamp) / 60000);
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
        var hFormat = d3.timeFormat("%H:%M");
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

    }

    function indicateNoDataForCell(normalisedName, thresholdName, reportWindowName) {
        var valueCellId = '.value_' + reportWindowName + '_' + normalisedName + '_' + thresholdName;
        d3.selectAll(valueCellId).classed('noDataReceived', true).
            classed('thresholdNotExceeded', false).
            classed('thresholdExceeded', false).
            text('-');
    }

    function _updateCell(normalisedName, thresholdName, reportWindowName, actualValue, thresholdValue, shouldDisplayValue) {
        var valueCellId = '.value_' + reportWindowName + '_' + normalisedName + '_' + thresholdName;
        var cell = d3.selectAll(valueCellId);
        var belowThreshold = actualValue < thresholdValue;
        cell.text(shouldDisplayValue ? actualValue : '');
        if(!shouldDisplayValue) {
            cell.attr('class', 'collapsed');
        } else {
            cell.classed('thresholdExceeded', !belowThreshold);
            cell.classed('thresholdNotExceeded', belowThreshold);
        }
        cell.classed('noDataReceived', false);
    }

    function windowDurationToDisplayText(reportWindow) {
        var displayText = reportWindow.duration + ' ';
        var pluralSuffix = 's';
        if(reportWindow.unit === 'DAYS') {
            displayText += 'day';
        }
        else if(reportWindow.unit === 'MINUTES') {
            displayText += 'min';
        }
        else if(reportWindow.unit === 'HOURS') {
            displayText += 'hr';
        }
        else if(reportWindow.unit === 'SECONDS') {
            displayText += 'sec';
            pluralSuffix = '';
        }

        if(parseInt(reportWindow.duration) !== 1) {
            displayText += pluralSuffix;
        }

        return displayText;
    }

    function toMetric(configuration) {
        var thresholdByName = {};
        var thresholdNames = [];
        for(var i = 0; i < configuration.metrics.length; i++) {
            thresholdByName[configuration.metrics[i].name] = parseInt(configuration.metrics[i].value);
            thresholdNames.push(configuration.metrics[i].name);
        }
        return {
            metricName: configuration.metricKey,
            normalisedName: configuration.metricKey.replace(/\./g, '_'),
            metricThresholds: configuration.metrics,
            thresholdByName: thresholdByName,
            thresholdNames: thresholdNames,

            getDisplayName: function() {
                return this.metricName;
            },

            getNormalisedName: function() {
                return this.normalisedName;
            },
            getMetricThresholds: function() {
                return this.metricThresholds;
            },
            getThresholdValue: function(metricName) {
                return this.thresholdByName[metricName];
            },
            getThresholdNames: function() {
                return this.thresholdNames;
            }
        }
    }

    function createView() {
        return {
            layout: function(metrics, layoutProperties, reportWindows) {


                for(var i = 0; i < metrics.length; i++) {
                    var container = d3.selectAll('.container').append('div').attr('class', 'metricContainer').
                                                classed('col-md-5', true);
                    var table = container.append('table').attr('class', 'metricTable');
                    var metric = metrics[i];
                    var displayName = metric.getDisplayName();
                    var normalisedName = metric.getNormalisedName();
                    if(displayPercentileCharts) {
                        container.append('div').append('canvas').attr('id', 'percentile_chart_' + normalisedName)
                    }
                    table.append('th').
                        attr('colspan', layoutProperties.columnCount + 1).
                        text(displayName);
                    var row = table.append('tr').attr('class', 'row_' + normalisedName);
                    row.append('td').text('');
                    for(var j = 0; j < metric.getMetricThresholds().length; j++) {
                        var threshold = metric.getMetricThresholds()[j];
                        row.append('td').attr('class', 'thresholdHeading threshold_' + normalisedName + '_' + threshold.name).
                                text(METRIC_NAME_TO_DISPLAY_NAME[threshold.name]);
                    }
                    row.append('td').attr('class', 'thresholdHeading threshold_' + normalisedName + '_COUNT').text('Count');
                    for(var k = 0; k < reportWindows.length; k++) {
                        var reportRow = table.append('tr').attr('class', '');
                        reportRow.append('td').
                            attr('class', 'reportWindowLabel label_' + normalisedName + '_' + reportWindows[k].duration + '_' + reportWindows[k].unit).
                            text(windowDurationToDisplayText(reportWindows[k]));
                        for(var m = 0; m < metric.getMetricThresholds().length; m++) {
                            var threshold = metric.getMetricThresholds()[m];
                            var valueCellId = 'value_' +
                                reportWindows[k].duration + '_' + reportWindows[k].unit + '_' +
                                normalisedName + '_' + threshold.name;
                            reportRow.append('td').
                                attr('class', valueCellId).
                                text('-');
                        }
                        reportRow.append('td').
                                attr('class', 'value_' +
                                      reportWindows[k].duration + '_' + reportWindows[k].unit + '_' +
                                      normalisedName + '_count').text('-');
                    }
                }
            }
        }
    }

    function loadLoop() {
        if(d3 && d3.json && d3.selectAll && typeof fluteUtil !== 'undefined') {
            flute_init();
        } else {
            setTimeout(loadLoop, 50);
        }
    }

    loadLoop();
})();
