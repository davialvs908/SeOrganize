(function () {
    'use strict';

    document.addEventListener('click', function (event) {
        var target = event.target;
        if (!target || typeof target.closest !== 'function') return;

        var excel = target.closest('a.js-excel-download');
        if (excel) {
            event.preventDefault();
            baixarExcel(excel);
            return;
        }

        var btn = target.closest('[data-toggle-password]');
        if (!btn) return;

        var inputId = btn.getAttribute('data-toggle-password');
        if (!inputId) return;

        var input = document.getElementById(inputId);
        if (!input) return;

        var mostrar = input.type === 'password';
        input.type = mostrar ? 'text' : 'password';
        btn.setAttribute('aria-pressed', mostrar ? 'true' : 'false');
        btn.setAttribute('aria-label', mostrar ? 'Ocultar senha' : 'Mostrar senha');

        var eyeOn = btn.querySelector('.eye-on');
        var eyeOff = btn.querySelector('.eye-off');
        if (eyeOn) eyeOn.style.display = mostrar ? 'none' : 'block';
        if (eyeOff) eyeOff.style.display = mostrar ? 'block' : 'none';
    });

    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') return;
        document.querySelectorAll('.modal-overlay.open').forEach(function (modal) {
            modal.classList.remove('open');
        });
    });

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!form || form.nodeName !== 'FORM') return;

        var label = form.getAttribute('data-loading-label');
        if (!label) return;

        var btn = event.submitter || form.querySelector('[type="submit"]');
        if (!btn || btn.disabled) return;

        btn.disabled = true;
        var inner = btn.querySelector('.btn-inner');
        if (inner) {
            inner.textContent = label;
        } else {
            btn.textContent = label;
        }
    });

    function baixarExcel(link) {
        var url = link.getAttribute('href');
        if (!url) return;

        var a = document.createElement('a');
        a.href = url;
        a.setAttribute('download', 'Relatorio_SeOrganize.xlsx');
        a.rel = 'noopener';
        document.body.appendChild(a);
        a.click();
        a.remove();
    }
})();
