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
(function outer_init() {
    var numberOfTimeWindows = 1;
    const ALL_THRESHOLDS = ['MEAN', 'FIFTIETH', 'TWO_NINES', 'THREE_NINES', 'FOUR_NINES', 'FIVE_NINES', 'MAX'];

    function hideBlock(selection) {
        selection.classed('displayBlock', false);
        selection.classed('displayNone', true);
    }

    function showBlock(selection) {
        selection.classed('displayBlock', true);
        selection.classed('displayNone', false);
    }

    function appendThreshold(thresholds, stat) {
        var statValue = d3.select('#' + stat).property('value');
        if(statValue.trim().length !== 0) {
            thresholds.push({percentile: stat, value: statValue});
        }
    }

    function appendTimeWindows(timeWindows) {
        for(var i = 0; i < numberOfTimeWindows; i++) {
            var unitSelector = '#twu_' + i;
            var durationSelector = '#twd_' + i;
            var window = {duration: d3.select(durationSelector).property('value'), unit: d3.select(unitSelector).property('value')};
            timeWindows.push(window);
        }
    }

    function flute_init() {
        d3.select('#createReportAction').on('click', function() {
            var reportSpecification = {};
            reportSpecification.reportName = d3.select('#reportName').property('value');
            reportSpecification.selectorPattern = '.*' + d3.select('#metricPattern').property('value') + '.*';
            reportSpecification.timeWindows = [];
            appendTimeWindows(reportSpecification.timeWindows);
            reportSpecification.thresholds = [];
            for(var i = 0; i < ALL_THRESHOLDS.length; i++) {
                appendThreshold(reportSpecification.thresholds, ALL_THRESHOLDS[i]);
            }

            fluteUtil.post('../../report/create/', reportSpecification,
                function(response) {
                    loadAndDisplayAvailableReports();
                },
                function(statusCode) {
                    console.log('error status: ' + statusCode);
                });

        });

        d3.select('#deleteReportAction').on('click', function() {
            d3.select('#deleteConfirmationLabel').classed('alertControl', false);
            var confirmed = d3.select('#deleteConfirmation').property('checked');
            if(!confirmed) {
                d3.select('.status').text('Please check the "confirm" control to delete.');
                d3.select('#deleteConfirmationLabel').classed('alertControl', true);
            } else {
                var reportName = d3.select('#reportName').property('value');
                fluteUtil.post('../../report/delete/' + reportName, {},
                    function(response) {
                        loadAndDisplayAvailableReports();
                    },
                    function(statusCode) {
                        console.log('error status: ' + statusCode);
                    });
            }
        });

        d3.select('#addTimeWindowAction').on('click', addTimeWindow);

        d3.select('#createReportLink').on('click', function() {
            hideBlock(d3.select('.listReportsPanel'));
            hideBlock(d3.select('.reportControlsMenu'));
            resetAndDisplayCreateReportForm();
        });

        d3.select('.metricSearch').on('keyup', function() {
            var searchTerm = '.*' + d3.select('.metricSearch').property('value') + '.*';
            fluteUtil.get('../../query/metricSearch/' + searchTerm,
                function(response) {
                    var matchingElements = '';
                    for(var i = 0; i < response.length; i++) {
                        matchingElements += '<span>' + response[i] + '</span><br/>';
                    }
                    d3.select('.matchingMetrics').html(matchingElements);
                }, function(statusCode) {
                    console.log('Status code', statusCode)
                });
        });

        var params = fluteUtil.urlParams();
        if(params['report'] && params['report'].length !== 0) {
            populateFormData(params['report'][0]);
        } else {
            loadAndDisplayAvailableReports();
        }
    }

    function addTimeWindow() {
        numberOfTimeWindows++;
        var containerIndex = numberOfTimeWindows - 1;
        d3.select('#timeWindowContainer').
            append('div').
            html(
                '<input type="text" id="twd_' + containerIndex + '" class="form-control"/>'+
                '<select id="twu_' + containerIndex + '" class="form-control">' +
                '<option  class="U_DAYS" value="DAYS">days</option>' +
                '<option  class="U_HOURS" value="HOURS">hours</option>' +
                '<option  class="U_MINUTES" value="MINUTES">minutes</option>' +
                '<option  class="U_SECONDS" value="SECONDS">seconds</option>' +
                '</select>');
    }

    function populateFormData(reportName) {
        fluteUtil.get('../../report/get/' + reportName, function(reportSpecification) {
            d3.select('#reportName').property('value', reportSpecification.reportName);
            d3.select('#reportName').property('readOnly', true);
            d3.select('#metricPattern').property('value', reportSpecification.selectorPattern);
            var i = 0;
            for(i = 0; i < reportSpecification.thresholds.length; i++) {
                var threshold = reportSpecification.thresholds[i];
                d3.select('#' + threshold.percentile).property('value', threshold.value);
            }
            numberOfTimeWindows = 1;
            for(i = 0; i < reportSpecification.timeWindows.length; i++) {
                var timeWindow = reportSpecification.timeWindows[i];
                if(i !== 0) {
                    addTimeWindow();
                }
                d3.select('#twd_' + i).property('value', timeWindow.duration);
                d3.select('#twu_' + i).select('.U_' + timeWindow.unit).property('selected', 'true');
            }

            showBlock(d3.select('.createReportPanel'));
            showBlock(d3.select('#amendmentControls'));

        }, function(statusCode) {
            console.log('Failed to retrieve report spec', statusCode);
        });
    }

    function loadAndDisplayAvailableReports() {
        fluteUtil.get('../../report/list', function(reportList) {
            if(reportList.length === 0) {
                hideBlock(d3.select('.listReportsPanel'));
                resetAndDisplayCreateReportForm();
            } else {
                hideBlock(d3.select('.createReportPanel'));
                var list = '<ul class="list-group">';
                for(var i = 0; i < reportList.length; i++) {
                    list += '<li class="list-group-item">' + reportList[i] + 
                            '<span class="badge" id="view_' + reportList[i] + '">view</span>' +
                            '<span class="badge" id="amend_' + reportList[i] + '">amend</span>' +
                            '</li>';
                }
                list += '</ul>';

                d3.select('.listReportsPanel').html(list);
                for(var i = 0; i < reportList.length; i++) {
                    d3.select('#view_' + reportList[i]).on('click', (function(reportName) {
                        return function() {
                            window.location.href = window.location.pathname.replace('reports', 'index') + '?report=' + reportName;
                        }
                    })(reportList[i]));
                    d3.select('#amend_' + reportList[i]).on('click', (function(reportName) {
                        return function() {
                            window.location.href = window.location.pathname + '?report=' + reportName;
                        }
                    })(reportList[i]));
                }

                showBlock(d3.select('.listReportsPanel'));
                showBlock(d3.select('.reportControlsMenu'));
            }
        }, function(statusCode) {
            console.log('Status code', statusCode)
        });
    }

    function resetAndDisplayCreateReportForm() {
        d3.select('#reportName').property('readOnly', false);
        showBlock(d3.select('.createReportPanel'));
        hideBlock(d3.select('#amendmentControls'));
        d3.select('#deleteConfirmation').property('checked', false);
        numberOfTimeWindows = 1;
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
