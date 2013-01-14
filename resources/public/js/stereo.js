var gutter = 8;
var left_url, right_url;
var orig_width, img_aspect;
var $mode = 'stereo-cross';

//
// Stereograms
//

var stereogram_position = function () {
  var width = $(window).width();
  var img_width = ((width - 2 * gutter) / 2);
  $('#left-img').width(img_width);
  $('#left-img').height(img_width * img_aspect);
  $('#right-img').width(img_width);
  $('#right-img').height(img_width * img_aspect);
};

var stereogram_prep = function () {
  if ($mode === 'stereo-cross') {
    $('#left-img').attr('src', right_url);
    $('#right-img').attr('src', left_url);
  } else {
    $('#left-img').attr('src', left_url);
    $('#right-img').attr('src', right_url);
  }

  stereogram_position();

  $('#left').css('float', 'left');
  $('#right').css('text-align', 'right');
  $('#right').removeClass('hidden');
};

//
// Wiggle
//

var wiggle_ms = 333;

var wiggle_position = function () {
  $('#right').css('left', ($(window).width() - orig_width)/2 + 'px');
}

var wiggle_prep = function () {
  $('#left').css('float', 'none');
  $('#right').addClass('hidden');
  $('#right').css('position', 'absolute');
  $('#right').css('top', gutter + 'px');

  wiggle_position();

  window.setTimeout('wiggle_tick();', wiggle_ms);
};

var wiggle_cleanup = function () {
  $('#left').css('float', 'left');
  $('#right').removeClass('hidden');
  $('#right').css('position', '');
  $('#right').css('top', '');
  $('#right').css('left', '');
}

var wiggle_tick = function () {
  if ($mode === 'wiggle') {
    $('#right').toggleClass('hidden');
    window.setTimeout('wiggle_tick();', wiggle_ms);
  }
};

//
// Anaglyph
//

var pix_cached = false;
var l_pix, r_pix;
var r_off, g_off, b_off, a_off;

var diff_channel = function (x_chans, y_chans) {
  var x_hits = [0,0,0,0];
  var y_hits = [0,0,0,0];
  for (var i = 0; i < 4; i++) {
    if (x_chans[i] >= 240) {
      x_hits[i] = 1;
    }
    if (y_chans[i] >= 240) {
      y_hits[i] = 1;
    }
  }

  for (var i = 0; i < 4; i++) {
    if (x_hits[i] === 1 && y_hits[i] === 0) {
      return i;
    }
  }
}

var like_channel = function (x_chans, y_chans) {
  var hits = [0,0,0,0];
  for (var i = 0; i < 4; i++) {
    if (x_chans[i] >= 240) {
      hits[i] += 1;
    }
    if (y_chans[i] >= 240) {
      hits[i] += 1;
    }
  }

  for (var i = 0; i < 4; i++) {
    if (hits[i] > 1) {
      return i;
    }
  }
}

var update_alignment_slider = function () {
  var canvas = document.getElementById('anaglyph-canvas');
  if (!canvas) return;

  var iw = $('#left-img').attr('nominal-width') * 1;
  var slider = document.getElementById('anaglyph-slider');
  slider.setAttribute('max', (canvas.width - iw)/2);
  slider.value = 40;
  if (window.fdSlider) {
    fdSlider.updateSlider('fd-slider-anaglyph-slider');
  }
}

var anaglyph_setup = function () {
  var r_img = new Image();
  var g_img = new Image();
  var b_img = new Image();
  var loaded = 0;

  var on_ref_load = function () {
    loaded += 1;
    if (loaded < 3) {
      return;
    }

    var r_data, g_data, b_data;
    var hidden_canvas = document.createElement('canvas');
    hidden_canvas.width = 1;
    hidden_canvas.height = 1;
    var hg = hidden_canvas.getContext('2d');

    hg.drawImage(r_img, 0, 0);
    r_data = hg.getImageData(0, 0, 1, 1).data;
    hg.drawImage(g_img, 0, 0);
    g_data = hg.getImageData(0, 0, 1, 1).data;
    hg.drawImage(b_img, 0, 0);
    b_data = hg.getImageData(0, 0, 1, 1).data;

    r_off = diff_channel(r_data, g_data);
    g_off = diff_channel(g_data, b_data);
    b_off = diff_channel(b_data, r_data);
    a_off = like_channel(r_data, g_data);

    var canvas = document.getElementById('anaglyph-canvas');
    if ($mode === 'anaglyph' && canvas) {
      update_alignment_slider();
      draw_anaglyph();
    }
  };

  r_img.onload = on_ref_load;
  b_img.onload = on_ref_load;
  g_img.onload = on_ref_load;

  r_img.src = '/img/red.png';
  g_img.src = '/img/green.png';
  b_img.src = '/img/blue.png';
};

var anaglyph_position = function () {
  var cw = $('#anaglyph-canvas')[0].width;
  var iw = $('#left-img').attr('nominal-width') * 1;

  $('#anaglyph').css('left', ($(window).width() - cw)/2 + 'px');

  var slider = document.getElementById('anaglyph-slider');
  slider.setAttribute('max', (cw - iw)/2);
};

var draw_anaglyph = function () {
  if (r_off === undefined) {
    return;
  }

  var l_img = document.getElementById('left-img');
  var r_img = document.getElementById('right-img');
  var iw = l_img.getAttribute('nominal-width') * 1;
  var ih = l_img.getAttribute('nominal-height') * 1;
  var i_pindex = function (x, y) { return 4*((y * iw) + x); };

  var canvas = document.getElementById('anaglyph-canvas');
  var g = canvas.getContext('2d');
  var cw = canvas.width;
  var ch = canvas.height;
  var c_pindex = function (x, y) { return 4*((y * cw) + x); };

  var anaglyph_gutter = (cw - iw)/2;
  var anaglyph_offset = document.getElementById('anaglyph-slider').value * 1;

  if (!pix_cached) {
    // load image pixel data
    g.drawImage(l_img, 0, 0);
    l_pix = g.getImageData(0, 0, iw, ih).data;
    g.drawImage(r_img, 0, 0);
    r_pix = g.getImageData(0, 0, iw, ih).data;
    pix_cached = true;
  }

  var anaglyph_image_data = g.createImageData(cw, ch);
  var a_pix = anaglyph_image_data.data;
  a_pix.length = l_pix.length;

  var l_x_off = anaglyph_gutter + anaglyph_offset;
  var r_x_off = anaglyph_gutter - anaglyph_offset;
  for (var y = 0; y < ih; y++) {
    for (var x = 0; x < iw; x++) {
      var src_index = i_pindex(x, y);
      var l_dest_index = c_pindex(x + l_x_off, y);
      var r_dest_index = c_pindex(x + r_x_off, y);

      // right image goes to red channel, left to blue & green
      a_pix[r_dest_index + r_off] = r_pix[src_index];
      a_pix[l_dest_index + g_off] = l_pix[src_index];
      a_pix[l_dest_index + b_off] = l_pix[src_index];
      // make both destination pixels fully opaque
      a_pix[r_dest_index + a_off] = 255;
      a_pix[l_dest_index + a_off] = 255;
    }
  }

  // draw the anaglyph
  g.putImageData(anaglyph_image_data, 0, 0);
};

var anaglyph_prep = function () {
  $('#left').addClass('hidden');
  $('#right').addClass('hidden');

  // see if the canvas is there already
  var canvas = document.getElementById('anaglyph-canvas');
  if (!canvas) {
    // if not, create canvas and stick it in the DOM
    canvas = document.createElement('canvas');
    canvas.id = 'anaglyph-canvas';
    canvas.width = $(window).width() - 20;
    canvas.height = $('#left-img').attr('nominal-height');
    $('#anaglyph').append(canvas);
  }

  var ih = $('#left-img').attr('nominal-height') * 1;
  $('#container').css('height', ih + gutter/2);

  update_alignment_slider();
  draw_anaglyph();

  $('#anaglyph').removeClass('hidden');
  $('.anaglyph-ui').removeClass('hidden');

  anaglyph_position();
};

var anaglyph_cleanup = function () {
  $('#anaglyph').addClass('hidden');
  $('.anaglyph-ui').addClass('hidden');
  $('#left').removeClass('hidden');
  $('#right').removeClass('hidden');
  $('#container').css('height', '');
};

//
// Mode Switching
//

var mode_prep = function () {
  if ($mode === 'stereo-cross' || $mode === 'stereo-wall') {
    stereogram_prep();
  } else {
    $('#left-img').width(orig_width);
    $('#left-img').height(orig_width * img_aspect);
    $('#right-img').width(orig_width);
    $('#right-img').height(orig_width * img_aspect);

    if ($mode === 'wiggle') {
      wiggle_prep();
    } else if ($mode === 'anaglyph') {
      anaglyph_prep();
    }
  }
};

var mode_cleanup = function (old_mode) {
  switch(old_mode) {
    case 'wiggle':
      wiggle_cleanup();
      break;
    case 'anaglyph':
      anaglyph_cleanup();
  }
}

var mode_select = function () {
  var new_mode = $('#mode-select').find(':selected').attr('value');
  var old_mode = $mode;

  mode_cleanup(old_mode);
  $mode = new_mode;
  mode_prep();

  document.cookie = 'stereo-mode=' + $mode + ';Expires=Sun, 17 Jan 2038'
                    + ' 19:14:07 GMT; path=/';
}

//
// Events
//

$(window).resize(function () {
  if ($mode === 'stereo-cross' || $mode === 'stereo-wall') {
    stereogram_position();
  } else if ($mode === 'wiggle') {
    wiggle_position();
  } else if ($mode === 'anaglyph') {
    anaglyph_position();
  }
});

var one_loaded = false;
var both_loaded = false;

var on_image_load = function () {
  if (one_loaded && !both_loaded) {
    both_loaded = true;
    mode_select();
  } else if (!one_loaded) {
    one_loaded = true;
  }
}

$(document).ready(function () {
  left_url = $('#left-img').attr('src');
  right_url = $('#right-img').attr('src');
  orig_width = $('#left-img').width();
  img_aspect = $('#left-img').height() / $('#left-img').width();

  $('#no-js').css('display', 'none');
  $('#mode-select').attr('disabled', false);
  $('option[value=' + $mode + ']').attr('selected', true);

  anaglyph_setup();

  mode_prep();
  $('#mode-select').change(mode_select);
  $('#anaglyph-slider').change(draw_anaglyph);

  $mode = $('#preferred-mode').attr('value');
  $('option[value=' + $mode + ']').attr('selected', true);

  var left_image = document.getElementById('left-img');
  var right_image = document.getElementById('right-img');
  left_image.onload = on_image_load;
  right_image.onload = on_image_load;
});

//
// HTML5 Input Range Polyfill for Friggin' Firefox
// See https://github.com/freqdec/fd-slider
//

Modernizr.load([
{ test: Modernizr.inputtypes.range
, nope: [ 'http://www.frequency-decoder.com/demo/fd-slider/css/fd-slider.mhtml.min.css'
        , 'http://www.frequency-decoder.com/demo/fd-slider/js/fd-slider.js'
        ]
, callback: function(id, testResult) {
    if ("fdSlider" in window && typeof (fdSlider.onDomReady) != "undefined") {
      try { fdSlider.onDomReady(); } catch(err) {};
    };
  }
}]);
