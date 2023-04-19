import Control from 'can-control';
import $ from 'jquery';

import User from 'models/user';
// import Country from 'models/country';

import template from './signup.stache';


export default Control.extend({

  "init": function(element, options) {
    $(element).hide();
    $(element).html(template());
    $(element).fadeIn();

    $("#terms-and-conditions").scroll(function() {
      var scrollTop = $(this).scrollTop(); // $("#terms-and-conditions").scrollTop()
      var tcHeight = $(this).height(); // $("#terms-and-conditions").height()
      var scrolled = Math.ceil(scrollTop + tcHeight);
      if (scrolled >= $(this)[0].scrollHeight) {
        $('#accept-terms-and-conditions')[0].disabled = false
      }
    });
  },

  'submit': function(element, event) {
    event.preventDefault();

    var that = this;
    var user = new User();

    // username
    var username = $(element).find("[name='username']");
    var usernameError = user.checkUsername(username.val());
    this.updateControl(username, usernameError);

    // fullname
    var fullname = $(element).find("[name='full-name']");
    var fullnameError = user.checkName(fullname.val());
    this.updateControl(fullname, fullnameError);

    // mail
    var mail = $(element).find("[name='mail']");
    var mailError = user.checkMail(mail.val());
    this.updateControl(mail, mailError);

    // password
    var newPassword = $(element).find("[name='new-password']");
    var confirmNewPassword = $(element).find("[name='confirm-new-password']");
    var passwordError = user.checkPassword(newPassword.val(), confirmNewPassword.val());
    this.updateControl(newPassword, passwordError);

    // institute name
    var instituteName = $(element).find("[name='institute-name']")
    var instituteNameError = (instituteName.val() !== "" ? undefined : 'Must input your institute name')
    this.updateControl(instituteName, instituteNameError);

    // institute country
    var instituteCountry = $(element).find("[name='institute-country']")
    var instituteCountryError = (instituteCountry.val() !== "" ? undefined : 'Must select a country for your institute')
    this.updateControl(instituteCountry, instituteCountryError);

    // terms & conditions
    var termsAndConditions = $(element).find("[name='accept-terms-and-conditions']") // document.querySelector('#accept-terms-and-conditions').checked;
    var termsAndConditionsError = (termsAndConditions[0].checked ? undefined : 'Must accept the terms & conditions')
    this.updateControl(termsAndConditions, termsAndConditionsError);

    if (usernameError || fullnameError || mailError || passwordError || termsAndConditionsError || instituteNameError || instituteCountryError) {
      return false;
    }

    $('#save').button('loading');

    $.ajax({
      url: "/api/v2/users/register",
      type: "POST",
      data: $(element).find("#signon-form").serialize(),
      dataType: 'json',
      success: function(data) {
        if (data.success == true) {
          // shows success
          $('#signon-form').hide();
          $('#success-message').show();
        } else {
          // shows error msg
          username = $('#signon-form').find("[name='username']");
          that.updateControl(username, data.message);
          $('#save').button('reset');

        }
      },
      error: function(message) {
        alert('failure: ' + message);
        $('#save').button('reset');
      }
    });

  },

  updateControl: function(control, error) {
    if (error) {
      control.removeClass('is-valid');
      control.addClass('is-invalid');
      control.closest('.form-group').find('.invalid-feedback').html(error);
    } else {
      control.removeClass('is-invalid');
      control.addClass('is-valid');
      control.closest('.form-group').find('.invalid-feedback').html('');
    }
  }

});
