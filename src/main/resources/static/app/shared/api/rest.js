const GET = async (url, bearer) => {
  try {
    const response = await fetch(url, {
      method: "GET", // *GET, POST, PUT, DELETE, etc.
      mode: "cors", // no-cors, *cors, same-origin
      cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
      credentials: "same-origin", // include, *same-origin, omit
      headers: getHeaders(bearer),
      redirect: "follow", // manual, *follow, error
      referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
      //body: JSON.stringify(data), // body data type must match "Content-Type" header
    });
    return response;
  } catch (err) {
    console.error(err);
    throw new Error(`Error on users, message is ${err.message}`);
  }
};

const POST = async (url, bearer, data) => {
  try {
    const response = await fetch(url, {
      method: "POST", // *GET, POST, PUT, DELETE, etc.
      mode: "cors", // no-cors, *cors, same-origin
      cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
      credentials: "same-origin", // include, *same-origin, omit
      headers: getHeaders(bearer),
      redirect: "follow", // manual, *follow, error
      referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
      body: getBody(data), // body data type must match "Content-Type" header
    });
    return response;
  } catch (err) {
    console.error(err);
    throw new Error(`Error on users, message is ${err.message}`);
  }
};

const PUT = async (url, bearer, data) => {
  try {
    const response = await fetch(url, {
      method: "PUT", // *GET, POST, PUT, DELETE, etc.
      mode: "cors", // no-cors, *cors, same-origin
      cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
      credentials: "same-origin", // include, *same-origin, omit
      headers: getHeaders(bearer),
      redirect: "follow", // manual, *follow, error
      referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
      body: getBody(data), // body data type must match "Content-Type" header
    });
    return response;
  } catch (err) {
    console.error(err);
    throw new Error(`Error on users, message is ${err.message}`);
  }
};

const PATCH = async (url, bearer, data) => {
  try {
    const response = await fetch(url, {
      method: "PATCH", // *GET, POST, PUT, DELETE, etc.
      mode: "cors", // no-cors, *cors, same-origin
      cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
      credentials: "same-origin", // include, *same-origin, omit
      headers: getHeaders(bearer),
      redirect: "follow", // manual, *follow, error
      referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
      body: getBody(data), // body data type must match "Content-Type" header
    });
    return response;
  } catch (err) {
    console.error(err);
    throw new Error(`Error on users, message is ${err.message}`);
  }
};

const getHeaders = (bearer) => {
  const headers = bearer
    ? {
        "Content-Type": "application/json",
        Authorization: "Bearer " + bearer,
        ...getSavedHeaders(),
        // 'Content-Type': 'application/x-www-form-urlencoded',
      }
    : {
        "Content-Type": "application/json",
        ...getSavedHeaders(),
        // 'Content-Type': 'application/x-www-form-urlencoded',
      };
  return headers;
};

const getBody = (data) => {
  const body = data ? JSON.stringify(data) : null;
  return body;
};
