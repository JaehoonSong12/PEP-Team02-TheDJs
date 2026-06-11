# API Contract Flow
## login
- client makes request
- POST /login with credentials
- API recieves request
- convert credentials into a Java object to validate in business layer
- Business Layer processes the credentials
- check if the username/password exists in database (by passing data to repo layer)
- if it does return a JWT for the user
- if it does not then return a 400 status code