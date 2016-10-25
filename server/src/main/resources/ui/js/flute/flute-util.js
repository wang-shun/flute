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
    function _get(url, onSuccess, onFailure) {
        d3.json(url).
            on("error", function(errorData) {
                if(typeof errorData == 'object' && errorData.target !== undefined) {
                    var statusCode = errorData.target.status;
                    onFailure(statusCode);
                } else {
                    onFailure(-1);
                }
            }).
            on("load", function(response) {
                onSuccess(response);
            }).
            get();
    };

    function _post(url, data, onSuccess, onFailure) {
        d3.json(url).
            on("error", function(errorData) {
                if(typeof errorData == 'object' && errorData.target !== undefined) {
                    var statusCode = errorData.target.status;
                    onFailure(statusCode);
                } else {
                    onFailure(-1);
                }
            }).
            on("load", function(response) {
                onSuccess(response);
            }).
            post(JSON.stringify(data));
    };

    function _urlParams() {
        var query = window.location.search;
        var params = {};
        if(query.length > 1)
        {
            var kvPairs = query.substring(1).split('&');
            for(var i = 0; i < kvPairs.length; i++) {
                var elements = kvPairs[i].split('=');
                if(typeof params[elements[0]] == 'undefined') {
                    params[elements[0]] = [];
                }

                if(elements.length > 1) {
                    params[elements[0]].push(elements[1]);
                }
            }
        }
        return params;
    };

    this.fluteUtil = {
        get: _get,
        post: _post,
        urlParams: _urlParams
    };
}(this));
