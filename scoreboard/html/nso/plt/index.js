WS.AfterLoad(function () {
  _windowFunctions.configureZoom();
  $('body')
    .attr('showTeam', _windowFunctions.getParam('team') || 'both')
    .attr('sbSheetStyle', _windowFunctions.getParam('pos') || 'plt');

  $('#OptionsDialog #OptionZoomable').prop('checked', _windowFunctions.checkParam('zoomable', 1)).button();
  $('#OptionsDialog [team="' + _windowFunctions.getParam('team') + '"]').addClass('sbActive');
  $('#OptionsDialog [pos="' + $('body').attr('sbSheetStyle') + '"]').addClass('sbActive');
  $('#OptionsDialog').dialog({
    modal: true,
    closeOnEscape: true,
    title: 'Option Editor',
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
    width: '500px',
    autoOpen: !_windowFunctions.hasParam('team'),
  });

  $('#UseLTDialog').dialog({
    modal: true,
    closeOnEscape: false,
    title: 'Use Lineup Tracking',
    buttons: {
      Enable: function () {
        WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)', true);
      },
    },
    width: '300px',
    autoOpen: false,
  });
  WS.Register(['ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)'], function (k, v) {
    $('#UseLTDialog').dialog(!isTrue(v) && $('body[sbSheetStyle*="lt"]').length ? 'open' : 'close');
  });
});

function toTitle() {
  const pos = _windowFunctions.getParam('pos').toUpperCase();
  const team = _windowFunctions.getParam('team') || 'both';
  const prefix = 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').Team(' + team + ').';
  return (
    pos +
    ' ' +
    (team === 'both'
      ? 'both'
      : WS.state[prefix + 'AlternateName(plt)'] || WS.state[prefix + 'UniformColor'] || WS.state[prefix + 'Name'] || '') +
    ' | CRG ScoreBoard'
  );
}

function updateTitle() {
  $('title').text(toTitle());
}

function openOptionsDialog() {
  $('#OptionsDialog').dialog('open');
}

function setTeam(k, v, elem) {
  $('#OptionsDialog [team]').removeClass('sbActive');
  elem.addClass('sbActive');
  $('body').attr('showTeam', elem.attr('team'));
  _sbUpdateUrl('team', elem.attr('team'));
  updateTitle();
}

function setPos(k, v, elem) {
  $('#OptionsDialog [pos]').removeClass('sbActive');
  elem.addClass('sbActive');
  $('body').attr('sbSheetStyle', elem.attr('pos'));
  _sbUpdateUrl('pos', elem.attr('pos'));
  updateTitle();
}

function setZoom(k, v, elem) {
  elem.toggleClass('sbActive');
  _sbUpdateUrl('zoomable', elem.filter('.sbActive').length);
  _windowFunctions.configureZoom();
}

function advanceFieldings(k) {
  const team = $('body').attr('showTeam');
  if (team === 'both') {
    WS.Set(k + '.Team(1).AdvanceFieldings', true);
    WS.Set(k + '.Team(2).AdvanceFieldings', true);
  } else {
    WS.Set(k + '.Team(' + team + ').AdvanceFieldings', true);
  }
}
