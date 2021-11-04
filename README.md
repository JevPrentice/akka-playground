# akka-playground

### Steps to run locally:
```
    docker compose up
    sbt run
```


### Example HTTP calls
```
   echo '{"itemId":"Test Product A", "quantity": 1, "keywordPhrases": ["dry"]}' | http post localhost:8080/api/product
   echo '{"itemId":"Test Product B", "quantity": 1, "keywordPhrases": ["moist"]}' | http post localhost:8080/api/product
   echo '{"itemId":"Test Product C", "quantity": 1, "keywordPhrases": ["qoily", "normal"]}' | http post localhost:8080/api/product
   echo '{"itemId":"Test Product D", "quantity": 1, "keywordPhrases": ["normal"]}' | http post localhost:8080/api/product
   echo '{"itemId":"Test Product E", "quantity": 1, "keywordPhrases": ["dry", "normal"]}' | http post localhost:8080/api/product
   
   http get localhost:8080/api/product
   http get localhost:8080/api/product/Test%20Product%20A
   http get localhost:8080/api/product
   http get localhost:8080/api/product/suggestion\?search\=dry
   
   http get https://spiderwalk-akka-playground.herokuapp.com/api/product
```
