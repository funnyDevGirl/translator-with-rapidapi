# translator-with-rapidapi
#### This is a Java web application for translating a set of words into another language using the Google translator using the Rapid API service.

[![Maintainability](https://api.codeclimate.com/v1/badges/18626dcd92b7b7efcab2/maintainability)](https://codeclimate.com/github/funnyDevGirl/translator-with-rapidapi/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/18626dcd92b7b7efcab2/test_coverage)](https://codeclimate.com/github/funnyDevGirl/translator-with-rapidapi/test_coverage)

---
### Start the application
Requirements:

* JDK 21
* Gradle

To run the program, run: ```make dev```

Compile the code: ```make install```

###
### Authentication in the Translate API
To work with the Translate API, you need to get the key from the Rapid API service.

For the convenience of testing, a temporary key is placed in ```application.yml```:
```yaml
api-key: ${API_KEY}
translate-url: "https://google-translator9.p.rapidapi.com/v2"
api-url: "https://google-translator9.p.rapidapi.com/v2/languages"
```
###
### Methods for the Translate service
* ```fetchSupportedLanguages()``` - Retrieves the list of supported languages:

HTTP-запрос
```
POST https://google-translator9.p.rapidapi.com/v2/languages
```

* ```translate()``` - Translates the text into the specified language:

HTTP-запрос
```
POST https://google-translator9.p.rapidapi.com/v2
```

```json
{
  "q": "Hello world, this is my first program",
  "source": "en",
  "target": "ru",
  "format": "text"
}
```