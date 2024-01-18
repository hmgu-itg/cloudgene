import Control from 'can-control';
import $ from 'jquery';
import md5 from 'md5';

import ErrorPage from 'helpers/error-page';
import Country from 'models/country';

import template from './country.stache';

export default Control.extend({

  "init": function(element, options) {

    // $(element).hide();
    // $(element).html(template());
    // $(element).fadeIn();

    var params = {};
    if (options.query) {
      params = {
        query: options.query
      }
    } else {
      params = {
        page: options.page
      }
    }

    Country.findAll(
      params,
      function(countries) {
        $(element).html(template({
          countries: countries,
          md5: md5,
          query: options.query
        }));
        $(element).fadeIn();
      },
      function(response) {
        new ErrorPage(element, response);
      });

  }
});
