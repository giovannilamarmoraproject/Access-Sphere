function deleteUser(identifier, username) {
  sweetalertConfirm(
    "question",
    currentTranslations.delete_user_question_title,
    currentTranslations.delete_user_question_text.replace("#USER#", username),
    currentTranslations.delete_user_btn_confirm,
    currentTranslations.delete_user_btn_deny
  ).then((result) => {
    /* Read more about isConfirmed, isDenied below */
    if (result.isConfirmed) {
      const token = getCookieOrStorage(config.access_token);
      const userDeleteUrl = config.delete_user_url + "/" + identifier; // URL per la modifica

      DELETE(userDeleteUrl, token).then(async (data) => {
        const responseData = await data.json();
        if (responseData.error != null) {
          const error = getErrorCode(responseData.error);
          return sweetalert("error", error.title, error.message);
        } else {
          fetchHeader(data.headers);
          localStorage.removeItem(config.client_id + "_usersData");
          return sweetalert(
            "success",
            currentTranslations.delete_user_success_title,
            currentTranslations.delete_user_success_text.replace(
              "#USER#",
              username
            )
          ).then((result) => {
            const origin = window.location.origin;

            // Costruisci l'URL completo aggiungendo il path
            const fullUrl = `${origin}/app/users`;

            // Reindirizza l'utente al nuovo URL
            window.location.href = fullUrl;
          });
        }
      });
    } else if (result.isDenied) {
      return sweetalert(
        "info",
        currentTranslations.delete_user_deny_title,
        currentTranslations.delete_user_deny_text
      );
    }
  });
}
