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

function get_start_index(files,n,chunk_sz){
    var x=0;
    var z=chunk_sz*n;
    for (let i=0;i<files.length;i++) {
        if (z>=x && z<x+files[i].size){
            return {"index":i,"offset":z-x};
        }
        x+=files[i].size;
    }
    return {"index":null,"offset":null};
}

function get_end_index(files,n,chunk_sz){
    var x=0;
    var z=chunk_sz*(n+1);
    for (let i=0;i<files.length;i++) {
        if (z>=x && z<x+files[i].size){
            return {"index":i,"offset":z-x};
        }
        x+=files[i].size;
    }
    return {"index":files.length-1,"offset":files[files.length-1].size};
}

function get_max_parts(files,n_chunks,chunk_sz){
    var part=1;
    var max_parts=1;

    for (let i=0;i<n_chunks;i++) {
        var z=get_start_index(inputFile.files,i,chunk_sz);
	var z2=get_end_index(inputFile.files,i,chunk_sz);

        if (z.index===z2.index){
            part++;
            if (part>max_parts){
                max_parts=part;
            }
        }
        else{
	    part=1;
            if (z2.offset!=0){
                part++;
                if (part>max_parts){
                    max_parts=part;
                }
            }
	}
    }

    return max_parts;
}

export default Control.extend({

    "init": function(element, options) {
	
	var that = this;
	
	var MAX_SIZE=1000*1024;
	// var total_size=0;
	// for (const file of inputFile.files)
	//     total_size+=file.size;

	// n_chunks=Math.floor(total_size/max_size);
	// if (total_size%max_size!=0){
	//     n_chunks+=1;
	// }

	// console.log("Total size: "+total_size+" bytes");
	// console.log("Chunks: "+n_chunks);

	// var starts=[];
	// var ends=[];
	// var part=1;
	// var max_parts=1;


    Application.findOne({
      tool: options.app
    }, function(application) {
      that.application = application;
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
      
    // check required parameters.
    if (form.checkValidity() === false) {
      form.classList.add('was-validated');
      return false;
    }

    //show upload dialog
    var uploadDialog = bootbox.dialog({
      message: templateUploadingDialog(),
      closeButton: false,
      className: 'upload-dialog',
      shown: false
    });

    //start uploading when dialog is shown
      uploadDialog.on('shown.bs.modal', function() {

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

	  // we do several uploads, each time we upload different file parts
	  
	  
      //submit form and upload files
      $(form).ajaxSubmit({
        dataType: 'json',
        headers: {
          "X-CSRF-Token": csrfToken
        },

        success: function(answer) {
          uploadDialog.modal('hide');
          if (answer.success) {
              window.location.href = '#!jobs/' + answer.id;
          } else {
            new ErrorPage("#content", {
              status: "",
              message: answer.message
            });
          }
        },

        error: function(response) {
          uploadDialog.modal('hide');
            new ErrorPage("#content", response);
        },

        //upade progress bar
        uploadProgress: function(event, position, total, percentComplete) {
          $("#waiting-progress").css("width", percentComplete + "%");
        }
      });
    });
    //show upload dialog. fires uploading files.
    uploadDialog.modal('show');
  },

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
	console.log("#select-files-btn click");
    // trigger click to open file dialog
    var fileUpload = $(button).parent().find(":file");
      fileUpload.trigger("click");
  },

    // 2.
    '.file-upload-field-multiple change': function(fileUpload) {
	console.log(".file-upload-field-multiple change");
    //update list of files
    var fileList = $(fileUpload).parent().find(".file-list");
    fileList.empty();
    for (var i = 0; i < fileUpload.files.length; i++) {
	fileList.append('<li><span class="fa-li"><i class="fas fa-file"></i></span>' + fileUpload.files[i].name + '</li>');
	console.log("file "+i+":"+fileUpload.files[i].name);
    }

    fileUpload.parent().find("#change-files");

    if (fileUpload.files.length > 0) {
      fileUpload.parent().find("#select-files").hide();
      fileUpload.parent().find("#change-files").show();
      fileUpload.parent().find("#remove-all-files").show();
    } else {
      fileUpload.parent().find("#select-files").show();
      fileUpload.parent().find("#change-files").hide();
      fileUpload.parent().find("#remove-all-files").hide();
    }
  },

  '#change-files-btn click': function(button) {
    // trigger click to open file dialog
      var fileUpload = $(button).parent().find(":file");
      console.log("#change-files-btn click");
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
