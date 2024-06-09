import Control from 'can-control';
import $ from 'jquery';
import Raphael from 'raphael/raphael';
import Morris from 'morris.js/morris.js';

import Counter from 'models/counter';
import template from './hadoop.stache';

export default Control.extend({
    "init": function(element, options) {
	$(element).hide();
	var that = this;
	window.Raphael = Raphael;

	Counter.findOne({}, function(counter) {
	    $(element).html(template({
		counter: counter
	    }));
	    $(element).fadeIn();

	    $.getJSON("/api/v2/admin/server/hadoop", {
		day: ""
	    }, function(mydata) {
		// all distinct dates		
		var dates=new Array();
		for (const x of mydata){
		    var s=x.T.split(" ")[0];
		    if (! dates.includes(s)){
			dates.push(s);
		    }
		}
		
		var select = document.getElementById('day_combo');
		for (const x of dates){
		    var opt = document.createElement('option');
		    opt.value = x;
		    opt.innerHTML = x;
		    select.appendChild(opt);
		    //console.log(x);
		}
		
		var metrics = document.getElementById('metrics_combo');
		for (const x of Object.keys(mydata[0]).sort()){
		    if (x=="T" || x.includes("CURSTATE"))
			continue;
		    var opt = document.createElement('option');
		    opt.value = x;
		    opt.innerHTML = x;
		    metrics.appendChild(opt);
		}

		var day = $("#day_combo").val();
		var m = $("#metrics_combo").val();
		that.options.running = Morris.Line({
		    element: 'morris-area-chart',
		    data: mydata.filter(function(x){return x.T.split(" ")[0]==day;}),
		    xkey: 'T',
		    ykeys: [m],
		    labels: [m],
		    pointSize: 0,
		    hideHover: 'always',
		    smooth: 'false',
		    resize: true
		});
	    });
	}, function(message) {
	});
    },

    '#day_combo change': function() {
	var day = $("#day_combo").val();
	var that = this;
	$.getJSON("/api/v2/admin/server/hadoop", {
	    day: day
	}, function(mydata) {
	    that.options.running.setData(mydata);
	});
    },

    '#metrics_combo change': function() {
	var metrics = $("#metrics_combo").val();
	var day = $("#day_combo").val();
	var that = this;
	console.log("day: "+day+" metrics: "+metrics);
	$("#morris-area-chart").html("");
	$.getJSON("/api/v2/admin/server/hadoop", {
	    day: day
	}, function(mydata) {
	    that.options.running = Morris.Line({
		element: 'morris-area-chart',
		data: mydata,
		xkey: 'T',
		ykeys: [metrics],
		labels: [metrics],
		pointSize: 0,
		hideHover: 'always',
		smooth: 'false',
		resize: true
	    });
	});
    }

});
