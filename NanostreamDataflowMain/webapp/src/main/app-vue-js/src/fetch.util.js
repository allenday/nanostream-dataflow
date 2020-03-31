function encodeURLData(params) {
    const formData = new FormData();
    Object.keys(params).map((key) => {
        formData.append(key, params[key])
    });
    return formData;
}

function handleServerError() {
    return async function (response) {
        console.log('fetch response', response)
        const text = await response.text();
        let result, isJson;
        try {
            result = JSON.parse(text);
            isJson = true;
        } catch {
            result = text;
            isJson = false;
        }
        if (response.ok && isJson) {
            return result;          // success response
        } else if (response.status === 400 && isJson && result && result.message) {
            console.error(response.statusText, result);
            throw result.message;   // server indicates an error
        } else {
            console.error(response.statusText, result);
            throw response.statusText + ' : See browser console for details';
        }
    };
}

function handleNetworkError() {
    return e => {
        console.error('fetch catch', e);
        if (e instanceof TypeError) {
            let message;
            if (window.navigator.onLine) {
                message = 'Network error: Please check your backend is running';
            } else {
                message = 'Network error: Please check your Internet connection';
            }
            throw message;
        } else {
            throw e;
        }
    };
}


export default {

    makeRequest(request) {
        return fetch(request)
            .then(handleServerError())
            .catch(handleNetworkError());
    },

}