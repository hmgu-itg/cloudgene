import Control from 'can-control';
import $ from 'jquery';

import template from './country.stache';

export default Control.extend({

  "init": function(element, options) {

    $(element).hide();
    $(element).html(template());
    $(element).fadeIn();
  }
});
