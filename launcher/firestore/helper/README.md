Purpose of this helper is perform some firestore admin operations not available in other tools.

Get firestore security rules currently possible by using nodejs api only:

See: 
- [Feature matrix by language](https://firebase.google.com/docs/admin/setup)
 
- [Manage and deploy Firebase Security Rules](https://firebase.google.com/docs/rules/manage-deploy)

- [Firebase Admin Node.js SDK Reference](https://firebase.google.com/docs/reference/admin/node)


To get current security rules use command:
```
node -e 'require("./main.js").get_security_rules()'
```
