import Control from 'can-control';
import domData from 'can-util/dom/data/data';
import $ from 'jquery';
import bootbox from 'bootbox';
import 'jquery-form';
import 'helpers/helpers';

import ErrorPage from 'helpers/error-page';
import Application from 'models/application';

import template from './submit.stache';
import templateUploadingDialog from './dialogs/uploading.stache';
import templateLabel from './controls/label.stache';
import templateSelect from './controls/select.stache';
import templateSelectBinded from './controls/select-binded.stache';
import templateRadio from './controls/radio.stache';
import templateCheckbox from './controls/checkbox.stache';
import templateFile from './controls/file.stache';
import templateFolder from './controls/folder.stache';
import templateFolderPattern from './controls/folder-pattern.stache';
import templateTermsCheckbox from './controls/terms-checkbox.stache';
import templateText from './controls/text.stache';
import templateTextarea from './controls/textarea.stache';

function getBaseLog(base,x) {
    return Math.log(x)/Math.log(base);
}

function add_input(ID,value,form){
    var el=document.getElementById(ID);
    if (el==null){
	el=document.createElement("input");
	form.appendChild(el);
    }
    el.setAttribute("type","hidden");
    el.setAttribute("value",value);
    el.setAttribute("name",ID);
    el.setAttribute("id",ID);
}

function createFormData(form,cc,nc,chunks,uploads,width){
    add_input("cur_chunk",cc+1,form);	
    add_input("total_chunks",nc,form);
    var fd=new FormData(form);
    //console.log(JSON.stringify(Object.fromEntries(fd)));
    //console.log("no of files: "+fd.get("files").length);
    fd.set("files",[]);
    //console.log(JSON.stringify(Object.fromEntries(fd)));
    //console.log("after resetting: no of files: "+fd.get("files").length);
    for (const r of chunks){
	if (r.chunk==cc){
	    console.log("chunk: "+r.chunk+" index="+r.index+" "+r.start+":"+r.end+", name="+r.name+", part="+r.part);
	    if (r.part==0)
		fd.append("files",uploads.files[r.index].slice(r.start,r.end),r.name);
	    else{
		var str=r.part;
		fd.append("files",uploads.files[r.index].slice(r.start,r.end),r.name+".part"+str.toString().padStart(width,'0'));
	    }
	}
    }

    //console.log("after updating: no of files: "+fd.get("files").length);
    return fd;
}

//--------------

function sendChunks(F,nc,uploads,chunks,token){
    let uploadDialog = bootbox.dialog({
	  message: templateUploadingDialog(),
	  closeButton: false,
	  className: 'upload-dialog',
	  shown: true
    });
    
    let promise=Promise.resolve();
    let new_loc;
    
    let max_parts=1;
    for (const r of chunks){
        if (r.part>max_parts)
	    max_parts=r.part;
    }
    let width=Math.floor(getBaseLog(10,max_parts))+1;
    // console.log("max_parts: "+max_parts);
    // console.log("width: "+w);
    // console.log("----------------------------------------------");
    // const delay = (fn, ms, ...args) => setTimeout(fn, ms, ...args);
    
    for (let i=0;i<nc;i++)
	promise=promise.then(()=>fetch(F.action,{headers:{"X-CSRF-Token":token},method:"post",body:createFormData(F,i,nc,chunks,uploads,width)}).then(response => {return response.text();}).then(data => {console.log("i: "+i+" DATA: "+data);console.log("i: "+i+" Received ID: "+JSON.parse(data).id);if (i==nc-1){$("#waiting-progress").css("width",100 + "%");uploadDialog.modal('hide');new_loc='#!jobs/'+JSON.parse(data).id;console.log("new location: "+new_loc);window.location.href=new_loc;} else {$("#waiting-progress").css("width",(i/(nc-1))*100 + "%");add_input("jobid",JSON.parse(data).id,F);add_input("lws",JSON.parse(data).lws,F);add_input("hws",JSON.parse(data).hws,F);}}).catch((error) => ("Something went wrong!", error)) );
}

//----------------

function split_files(files,chunk_sz){
    var r=new Array();
    var cur_c=0;
    var C;
    var sz_left=chunk_sz;
    var p=1;

    for (let i=0;i<files.length;i++){
        var off=0;
        if (files[i].size<=sz_left){
            r.push({"chunk":cur_c,"index":i,"start":0,"end":files[i].size,"name":files[i].name,"part":0});
            sz_left-=files[i].size;
        }
	else{
            if (sz_left>0){
	        r.push({"chunk":cur_c,"index":i,"start":0,"end":sz_left,"name":files[i].name,"part":p});
                p++;
            }
            var fsz=files[i].size-sz_left;
            off=sz_left;
            cur_c++;
            sz_left=chunk_sz;
            while(fsz>0){
		if (sz_left>=fsz){
                    r.push({"chunk":cur_c,"index":i,"start":off,"end":off+fsz,"name":files[i].name,"part":p});
                    p=1;
                    sz_left-=fsz;
                    fsz=0;
                }
                else{
                    r.push({"chunk":cur_c,"index":i,"start":off,"end":off+sz_left,"name":files[i].name,"part":p});
                    off+=sz_left;
                    fsz-=sz_left;
                    p++;
	            cur_c++;
                    sz_left=chunk_sz;
		}
            }
        }
    }

    return r;
}

export default Control.extend({
    "init": function(element, options) {
	var that = this;
	
	Application.findOne({
	    tool: options.app
	}, function(application) {
	    that.application = application;
	    //console.log(application);
	    $(element).hide();
	    $(element).html(template({
		application: application,
		controls_label: templateLabel,
		controls_select: templateSelect,
		controls_radio: templateRadio,
		controls_text: templateText,
		controls_checkbox: templateCheckbox,
		controls_file: templateFile,
		controls_folder: templateFolder,
		controls_folder_pattern: templateFolderPattern,
		controls_terms_checkbox: templateTermsCheckbox,
		controls_textarea: templateTextarea,
		controls_select_binded: templateSelectBinded
	    }));
	    $(element).fadeIn();
	    $("select").change();

	}, function(response) {
	    new ErrorPage(element, response);
	});
    },

    '#parameters submit': function(form, event) {
	event.preventDefault();

	// max chunk size, bytes
	var MAX_CHUNK_SIZE=1000*1024;
	var total_size=0;
	var n_chunks=0;
      
	// check required parameters.
	if (form.checkValidity() === false) {
	    form.classList.add('was-validated');
	    return false;
	}

	console.log("-----------");
	console.log("FORM ID: "+form.id);
	var fileUpload=null;
	for (const x of form.elements){
	    console.log("element ID: "+x.id);
	    if (x.id==="files")
		fileUpload=x;
	}
	console.log("TOTAL FILES: "+fileUpload.files.length);
	console.log("-----------");
	console.log("MAX_CHUNK_SIZE: "+MAX_CHUNK_SIZE);
	total_size=0;
	for (const file of fileUpload.files)
	    total_size+=file.size;
	console.log("total size: "+total_size);
	n_chunks=Math.floor(total_size/MAX_CHUNK_SIZE);
	if (total_size%MAX_CHUNK_SIZE!=0){
	    n_chunks+=1;
	}
	console.log("chunks: "+n_chunks);
	
	var csrfToken;
	if (localStorage.getItem("cloudgene")) {
            try {
		// get data
		var data = JSON.parse(localStorage.getItem("cloudgene"));	    
		csrfToken = data.csrf;
            } catch (e) {
		//do nothing.
            }
	}

	var R2=split_files(fileUpload.files,MAX_CHUNK_SIZE);
	add_input("jobid","NA",form);	
	add_input("lws","NA",form);	
	add_input("hws","NA",form);
	add_input("total_chunks",n_chunks,form);
	
	sendChunks(form,n_chunks,fileUpload,R2,csrfToken);	
  }, // # parameters submit

  // custom file upload controls for single files
  '.select-control change': function(){
    this.application.updateBinding();
  },

    '#select-single-file-btn click': function(button) {
	console.log("#select-single-file-btn click");
    // trigger click to open file dialog
    var fileUpload = $(button).closest('.col-sm-3').find(":file");
    fileUpload.trigger("click");
  },

    '.file-upload-field-single change': function(fileUpload) {
	console.log(".file-upload-field-single change");
    var filenameControl = $(fileUpload).parent().find(".file-name-control");
    if (fileUpload.files.length > 0) {
      filenameControl.val(fileUpload.files[0].name);
    } else {
      filenameControl.val('');
    }
  },

    // custom file upload controls for multiple files
    // 1.
    '#select-files-btn click': function(button) {
	//console.log("#select-files-btn click");
    // trigger click to open file dialog
	var fileUpload = $(button).parent().find(":file");
	this.input_files=fileUpload;
      fileUpload.trigger("click");
  },

    // 2.
    '.file-upload-field-multiple change': function(fileUpload) {
	//console.log(".file-upload-field-multiple change");
	//update list of files
	var fileList = $(fileUpload).parent().find(".file-list");
	fileList.empty();
	for (var i = 0; i < fileUpload.files.length; i++) {
	    fileList.append('<li><span class="fa-li"><i class="fas fa-file"></i></span>' + fileUpload.files[i].name + '</li>');
	    //console.log("file "+i+":"+fileUpload.files[i].name);
	}
	
	// this part doesn't do anything
	// -------------------------------
	// fileUpload.parent().find("#change-files");

	// if (fileUpload.files.length > 0) {
	//   fileUpload.parent().find("#select-files").hide();
	//   fileUpload.parent().find("#change-files").show();
	//   fileUpload.parent().find("#remove-all-files").show();
	// } else {
	//   fileUpload.parent().find("#select-files").show();
	//   fileUpload.parent().find("#change-files").hide();
	//   fileUpload.parent().find("#remove-all-files").hide();
	// }
	// -------------------------------	
    },

  '#change-files-btn click': function(button) {
    // trigger click to open file dialog
      var fileUpload = $(button).parent().find(":file");
      //console.log("#change-files-btn click");
    fileUpload.trigger("click");
  },

  '#remove-all-files-btn click': function(button) {
    //clear hidden file upload field
    var fileUpload = $(button).parent().find(":file");
    fileUpload.val('');
    //clear list of files
    var fileList = $(button).parent().find(".file-list");
    fileList.empty();
    fileUpload.parent().find("#select-files").show();
    fileUpload.parent().find("#change-files").hide();
    fileUpload.parent().find("#remove-all-files").hide();
  }

});
