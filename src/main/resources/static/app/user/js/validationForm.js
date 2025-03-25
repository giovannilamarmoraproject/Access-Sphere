$(document).ready(function () {
  // Seleziona il bottone submit
  let $submitButton = $("button[type='submit']");
  // Seleziona il checkbox "Agree to terms"
  let $termsCheckbox = $("#invalidCheck2");

  // Disabilita il bottone inizialmente
  $submitButton.prop("disabled", true);

  // Funzione per verificare se tutti i campi required sono compilati
  function checkRequiredFields() {
    let allFilled = true;

    // Controlla tutti gli input, select e textarea con required
    $("input[required], select[required], textarea[required]").each(
      function () {
        //console.log($(this), $(this).val());
        if ($(this).val() === null || $(this).val().trim() === "") {
          allFilled = false;
          return false; // Esce dal ciclo se trova un campo vuoto
        }
      }
    );

    // Verifica anche se il checkbox "terms and conditions" è selezionato
    let termsChecked = $termsCheckbox.is(":checked");

    /**
     *  Verifica se almeno un ruolo è presente
     */
    let hasRole = $(".role-card").length > 0;

    // Abilita o disabilita il bottone in base alla validità dei campi e al checkbox
    $submitButton.prop("disabled", !(allFilled && termsChecked && hasRole));
  }

  // Ascolta gli eventi di input sui campi required
  $("input[required], select[required], textarea[required]").on(
    "input change",
    function () {
      checkRequiredFields();
    }
  );

  // Ascolta il cambiamento dello stato del checkbox "terms and conditions"
  $termsCheckbox.on("change", function () {
    checkRequiredFields();
  });

  let $passwordInput = $("#password");
  // Ascolta l'evento di input sulla password
  $passwordInput.on("input", function () {
    validatePasswords();
  });
  let $passwordConfirmInput = $("#confirm_password");
  $passwordConfirmInput.on("input", function () {
    validatePasswords();
  });

  /**
   * Controllo se i ruoli sono stati cliccati
   */
  $("#add_role_btn").on("click", function () {
    checkRequiredFields();
  });

  $(document).on("click", ".remove-role", function () {
    checkRequiredFields();
  });
  /**
   * END Controllo se i ruoli sono stati cliccati
   */

  // Controllo iniziale nel caso in cui alcuni campi siano già compilati
  checkRequiredFields();

  function validatePasswords() {
    // Seleziona l'input della password e il messaggio di feedback
    let $passwordInput = $("#password");
    let $passwordFeedback = $("#validationPassword");
    let $passwordConfirmInput = $("#confirm_password");
    let $passwordConfirmFeedback = $("#validationConfirmPassword");

    // La regex per la password
    const passwordRegex =
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.,])[A-Za-z\d@$!%*?&.,]{8,20}$/;

    // Funzione per verificare la password
    function validatePassword() {
      let password = $passwordInput.val();

      // Controlla se la password rispetta la regex
      if (passwordRegex.test(password)) {
        // Se la password è valida, rimuove il feedback di errore e la classe 'is-invalid'
        $passwordInput.removeClass("is-invalid");
        $passwordFeedback.hide(); // Nasconde il messaggio di errore
      } else {
        // Se la password non è valida, aggiunge la classe 'is-invalid' e mostra il messaggio di errore
        $passwordInput.addClass("is-invalid");
        $passwordFeedback.show(); // Mostra il messaggio di errore
      }

      let passwordConfirm = $passwordConfirmInput.val();
      if (password != passwordConfirm) {
        if (passwordConfirm != "") {
          $passwordConfirmInput.addClass("is-invalid");
          $passwordConfirmFeedback.show(); // Mostra il messaggio di errore
        }
      } else {
        $passwordConfirmInput.removeClass("is-invalid");
        $passwordConfirmFeedback.hide(); // Mostra il messaggio di errore
      }
    }

    validatePassword();
  }
});
