(function () {
  _windowFunctions.configureZoom();
  $('body').attr('showTeam', _windowFunctions.getParam('team'));

  $('#OptionsDialog #OptionZoomable').prop('checked', _windowFunctions.checkParam('zoomable', 1)).button();
  $('#OptionsDialog [team="' + _windowFunctions.getParam('team') + '"]').addClass('sbActive');
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
})();

function toTitle() {
  const team = $('body').attr('showTeam');
  const prefix = 'ScoreBoard.Game(' + _windowFunctions.getParam('game') + ').Team(' + team + ').';
  return (
    'SK ' +
    (team === 'both'
      ? 'both'
      : WS.state[prefix + 'AlternateName(operator)'] || WS.state[prefix + 'UniformColor'] || WS.state[prefix + 'Name'] || '') +
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

function setZoom(k, v, elem) {
  elem.toggleClass('sbActive');
  _sbUpdateUrl('zoomable', elem.filter('.sbActive').length);
  _windowFunctions.configureZoom();
}
