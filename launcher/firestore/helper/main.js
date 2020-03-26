const admin = require('firebase-admin');

module.exports = {
    get_security_rules: function get_security_rules() {
        const app = admin.initializeApp();
        app.securityRules().getFirestoreRuleset()
            .then(function (result) {
                // console.log('result', result)
                console.log(result.source[0].content);
                app.delete();
            })
            .catch(function (error) {
                console.error('error', error);
                process.exit(0); // return 0 to not break execution of install script
            });
    }
};