import Control from 'can-control';
import $ from 'jquery';

import User from 'models/user';
import Country from 'models/country-signup';

import template from './signup.stache';
import "./signup.css";

export default Control.extend({
  "init": function(element, options) {
    var params = {};
    Country.findAll(
      params,
      function(countries) {
        $(element).html(template({
          countries: countries
        }));
        $(element).fadeIn();
      },
      function(response) {
        new ErrorPage(element, response);
      });

    // $(element).hide();
    // $(element).html(template());
    // $(element).fadeIn();

    $(document).on("click", "#tos-view-btn", function () {
      $("#terms-and-conditions").removeClass("hidden");
    });

    $(document).on("click", ".close-btn", function () {
      $("#terms-and-conditions").addClass("hidden");
    });

    // We use the MutationObserver to detect that the .content under #terms-and-conditions has been added to the DOM
    var observer = new MutationObserver(function(mutations) {
      mutations.forEach(function(mutation) {
        if (mutation.type === "childList" && mutation.addedNodes.length > 0) {
          var contentElement = document.querySelector("#terms-and-conditions .tac-background .content");
          if (contentElement) {
            contentElement.addEventListener("scroll", function() {
              var scrollTop = this.scrollTop;
              var tcHeight = this.clientHeight;
              var scrolled = Math.ceil(scrollTop + tcHeight);
              var scrollHeight = this.scrollHeight;
              console.log(scrolled)
              if (scrolled >= scrollHeight) {
                document.querySelector('#accept-terms-and-conditions').disabled = false;
                document.querySelector('#accept-eu').disabled = false;
              }
            });
            observer.disconnect();
          }
        }
      });
    });
    observer.observe(document.body, { childList: true, subtree: true });
    // setTimeout(function() {
    //   var contentElement = document.querySelector("#terms-and-conditions .tac-background .content");
    //   console.log(contentElement); // Debugging statement
    //   contentElement.addEventListener("scroll", function() {
    //     var scrollTop = this.scrollTop;
    //     var tcHeight = this.clientHeight;
    //     var scrolled = Math.ceil(scrollTop + tcHeight + 2);
    //     var scrollHeight = this.scrollHeight;
    //     console.log(scrolled);
    //     console.log(scrollHeight);
    //     if (scrolled >= scrollHeight) {
    //       document.querySelector('#accept-terms-and-conditions').disabled = false;
    //       document.querySelector('#accept-eu').disabled = false;
    //     }
    //   });
    // }, 1000);
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

    // institute email
    var instituteEmail = $(element).find("[name='institute-mail']")
    var instituteEmailError = (instituteEmail.val() !== "" ? undefined : 'Must input the email of your institute supervisor/legal-representative')
    this.updateControl(instituteEmail, instituteEmailError);

    // institute name
    var instituteName = $(element).find("[name='institute-name']")
    var instituteNameError = (instituteName.val() !== "" ? undefined : 'Must input your institute name')
    this.updateControl(instituteName, instituteNameError);

    // institute address
    var instituteAddress1 = $(element).find("[name='institute-address1']")
    var instituteAddress1Error = (instituteAddress1.val() !== "" ? undefined : 'Must input your institute address')
    this.updateControl(instituteAddress1, instituteAddress1Error);

    // institute city
    var instituteCity = $(element).find("[name='institute-city']")
    var instituteCityError = (instituteCity.val() !== "" ? undefined : 'Must input your institute city')
    this.updateControl(instituteCity, instituteCityError);

    // institute postal code
    var institutePostCode = $(element).find("[name='institute-postcode']")
    var institutePostCodeError = (institutePostCode.val() !== "" ? undefined : 'Must input your institute postal code')
    this.updateControl(institutePostCode, institutePostCodeError);

    // institute country
    var instituteCountry = $(element).find("[name='institute-country']")
    var instituteCountryError = (instituteCountry.val() !== "" ? undefined : 'Must select a country for your institute')
    this.updateControl(instituteCountry, instituteCountryError);

    // terms & conditions
    var termsAndConditions = $(element).find("[name='accept-terms-and-conditions']") // document.querySelector('#accept-terms-and-conditions').checked;
    var termsAndConditionsError = (termsAndConditions[0].checked ? undefined : 'Must accept the terms & conditions')
    this.updateControl(termsAndConditions, termsAndConditionsError);

    // EU-/EAA-country
    var termsAndConditionsCountry = $(element).find("[name='accept-eu']")
    var termsAndConditionsCountryError = (termsAndConditionsCountry[0].checked ? undefined : 'Must agree to only use the service within the EU-/EEA-country')
    this.updateControl(termsAndConditionsCountry, termsAndConditionsCountryError);

    // institute Permission
    var termsAndConditionsPermission = $(element).find("[name='accept-permission']")
    var termsAndConditionsPermissionError = (termsAndConditionsPermission[0].checked ? undefined : 'Must agree that you are a representative of your institute')
    this.updateControl(termsAndConditionsPermission, termsAndConditionsPermissionError);

    if (usernameError || fullnameError || mailError || passwordError || instituteEmailError || instituteNameError || instituteAddress1Error || instituteCityError || institutePostCodeError || instituteCountryError || termsAndConditionsError || termsAndConditionsCountryError || termsAndConditionsPermissionError) {
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
