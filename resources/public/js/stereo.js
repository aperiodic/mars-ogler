var gutter = 8;
var left_url, right_url;
var orig_width, img_aspect;
var $mode = 'stereo-cross';

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

var wiggle_tick = function () {
  if ($mode === 'wiggle') {
    $('#right').toggleClass('hidden');
    window.setTimeout('wiggle_tick();', wiggle_ms);
  }
};

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
    }
  }
};

$(document).ready(function(){
  left_url = $('#left-img').attr('src');
  right_url = $('#right-img').attr('src');
  orig_width = $('#left-img').width();
  img_aspect = $('#left-img').height() / $('#left-img').width();

  $('#no-js').css('display', 'none');
  $('#mode-select').attr('disabled', false);
  $('option[value=' + $mode + ']').attr('selected', true);

  mode_prep();
});

$(window).resize(function(){
  if ($mode === 'stereo-cross' || $mode === 'stereo-wall') {
    stereogram_position();
  } else if ($mode === 'wiggle') {
    wiggle_position();
  }
});
