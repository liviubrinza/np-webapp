/*
 * Central flatpickr configuration for every calendar in the app. Include it through the
 * `fragments/datepicker :: scripts` fragment and create pickers with a plain flatpickr(...)
 * call - these defaults already apply, so a new calendar looks and behaves like the others
 * without repeating any of them. Loaded after the optional locale bundle on purpose: each
 * bundle ships its own firstDayOfWeek, which this file overrides.
 */
(function () {
    var locale = (document.currentScript && document.currentScript.dataset.locale) || 'default';

    // Monday-first, regardless of what the default or a locale bundle says.
    Object.keys(flatpickr.l10ns).forEach(function (key) {
        flatpickr.l10ns[key].firstDayOfWeek = 1;
    });

    flatpickr.setDefaults({
        // The input keeps the ISO value the server binds; the visible alt input shows dd-Mon-yyyy.
        dateFormat: 'Y-m-d',
        altInput: true,
        altFormat: 'd-M-Y',
        locale: flatpickr.l10ns[locale] ? locale : 'default'
    });
})();
