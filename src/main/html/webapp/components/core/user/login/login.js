import Control from 'can-control';
import $ from 'jquery';

import ErrorPage from 'helpers/error-page';
import template from './login.stache';


export default Control.extend({

  "init": function (element, options) {
    $(element).hide();
    $(element).html(template());
    $(element).fadeIn();
  },

  //'#signin-form submit': function (element, event) {
  'submit': function (element, event) {
    event.preventDefault();

    var password = $(element).find("[name='loginPassword']");
    var OTP = $(element).find("[name='otpInput']");

    $.ajax({
      url: "/login",
      type: "POST",
      data: $(element).find("#signin-form").serialize(),
      dataType: 'json',
      success: function (response) {
          if (response.success == true) {
	      if (response.otpRequired == false){
		  //save CSRF token to local storage
		  var dataToken = {
		      csrf: response.csrf
		  };
		  localStorage.setItem('cloudgene', JSON.stringify(dataToken));
		  var redirect = '/';
		  if (response.roles.includes('block')) {
		      redirect = '/#pages/block';
		      window.location = redirect;
		  } else if (response.roles == '') {
		      // Show standard contractual clause info page
		      redirect = '/#pages/scc-info';
		      window.location = redirect;
		  } else {
		      // Normal login. redirect to home page
		      window.location = redirect;
		  }
	      }
	      else{
		  $('#otp-form-group').show();
	      }
        } else {
          // shows error
            var message = response.message;
	    if (/time password/.test(message)){
		OTP.addClass('is-invalid');
		OTP.closest('.form-group').find('.invalid-feedback').html(message);
	    }
	    else{
		password.addClass('is-invalid');
		password.closest('.form-group').find('.invalid-feedback').html(message);
	    }
        }
      },
      error: function (response) {
        new ErrorPage(element, response);
      }
    });

  }

});
