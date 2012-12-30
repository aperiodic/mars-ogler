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

var have_anaglyph = false;

var anaglyph_position = function () {
  $('#anaglyph').css('left', ($(window).width() - orig_width)/2 + 'px');
}

var anaglyph_prep = function () {
  var img_h = $('#left-img')[0].height;
  $('#left').addClass('hidden');
  $('#right').addClass('hidden');

  if (!have_anaglyph) {
    var l_img = document.getElementById('left-img');
    var r_img = document.getElementById('right-img');
    var canvas = document.getElementById('anaglyph-canvas');
    var g = canvas.getContext('2d');
    var w = canvas.width;
    var h = canvas.height;

    // load image pixel data
    g.drawImage(l_img, 0, 0);
    var l_pixels = g.getImageData(0, 0, w, h).data;
    g.drawImage(r_img, 0, 0);
    var r_pixels = g.getImageData(0, 0, w, h).data;

    // build anaglyph pixel data -- left in red channel, right in blue & green
    var anaglyph_image_data = g.createImageData(w, h);
    var anaglyph_pixels = anaglyph_image_data.data;
    anaglyph_pixels.length = l_pixels.length;
    for (var i = 0; i < l_pixels.length; i += 4) {
      anaglyph_pixels[i] = l_pixels[i];
      anaglyph_pixels[i+1] = r_pixels[i];
      anaglyph_pixels[i+2] = r_pixels[i];
      anaglyph_pixels[i+3] = 255;
    }

    // draw anaglyph
    g.putImageData(anaglyph_image_data, 0, 0);

    have_anaglyph = true;
  }

  $('#container').css('height', img_h + gutter/2);
  $('#anaglyph').removeClass('hidden');

  anaglyph_position();
};

var anaglyph_cleanup = function () {
  $('#anaglyph').addClass('hidden');
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

$(document).ready(function () {
  left_url = $('#left-img').attr('src');
  right_url = $('#right-img').attr('src');
  orig_width = $('#left-img').width();
  img_aspect = $('#left-img').height() / $('#left-img').width();

  $('#no-js').css('display', 'none');
  $('#mode-select').attr('disabled', false);
  $('option[value=' + $mode + ']').attr('selected', true);

  mode_prep();
  $('#mode-select').change(mode_select);
});
