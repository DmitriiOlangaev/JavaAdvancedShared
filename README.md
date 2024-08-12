# Тесты к курсу «Технологии Java»

[Условия домашних заданий](https://www.kgeorgiy.info/courses/java-advanced/homeworks.html)


## Домашнее задание 9. HelloUDP

Интерфейсы

 * `HelloUDPClient` должен реализовывать интерфейс
    [HelloClient](modules/info.kgeorgiy.java.advanced.hello/info/kgeorgiy/java/advanced/hello/HelloClient.java)
 * `HelloUDPServer` должен реализовывать интерфейс
    [HelloServer](modules/info.kgeorgiy.java.advanced.hello/info/kgeorgiy/java/advanced/hello/HelloServer.java)

Тестирование

 * простой вариант (`client` и `server`)
 * сложный вариант (`client-i18n` и `server-i18n`)
    * на противоположной стороне находится система, дающая ответы на различных языках
 * продвинутый вариант (`client-evil` и `server-evil`)
    * на противоположной стороне находится старая система,
      не полностью соответствующая последней версии спецификации

Тестовый модуль: [info.kgeorgiy.java.advanced.hello](artifacts/info.kgeorgiy.java.advanced.hello.jar)

Исходный код тестов:

* [Клиент](modules/info.kgeorgiy.java.advanced.hello/info/kgeorgiy/java/advanced/hello/HelloClientTest.java)
* [Сервер](modules/info.kgeorgiy.java.advanced.hello/info/kgeorgiy/java/advanced/hello/HelloServerTest.java)

Решение:
* [Клиент](src/info/kgeorgiy/ja/olangaev/hello/HelloUDPClient.java)
* [Сервер](src/info/kgeorgiy/ja/olangaev/hello/HelloUDPServer.java)


## Домашнее задание 8. Web Crawler

Тесты используют только внутренние данные и ничего не скачивают из интернета.

Тестирование
 * простой вариант (`easy`):
    [тесты](modules/info.kgeorgiy.java.advanced.crawler/info/kgeorgiy/java/advanced/crawler/EasyCrawlerTest.java)
 * сложный вариант (`hard`):
    [тесты](modules/info.kgeorgiy.java.advanced.crawler/info/kgeorgiy/java/advanced/crawler/HardCrawlerTest.java)
 * продвинутый вариант (`advanced`): 
    [интерфейс](modules/info.kgeorgiy.java.advanced.crawler/info/kgeorgiy/java/advanced/crawler/AdvancedCrawler.java),
    [тесты](modules/info.kgeorgiy.java.advanced.crawler/info/kgeorgiy/java/advanced/crawler/AdvancedCrawlerTest.java)

[Интерфейсы и вспомогательные классы](modules/info.kgeorgiy.java.advanced.crawler/info/kgeorgiy/java/advanced/crawler/)

Тестовый модуль: [info.kgeorgiy.java.advanced.crawler](artifacts/info.kgeorgiy.java.advanced.crawler.jar)

Решение:
* [WebCrawler](src/info/kgeorgiy/ja/olangaev/crawler/WebCrawler.java)


## Домашнее задание 7. Параллельный запуск

Тестирование
 * простой вариант (`scalar`):
    [тесты](modules/info.kgeorgiy.java.advanced.mapper/info/kgeorgiy/java/advanced/mapper/ScalarMapperTest.java)
 * сложный вариант (`list`):
    [тесты](modules/info.kgeorgiy.java.advanced.mapper/info/kgeorgiy/java/advanced/mapper/ListMapperTest.java)
 * продвинутый вариант (`advanced`):
    [тесты](modules/info.kgeorgiy.java.advanced.mapper/info/kgeorgiy/java/advanced/mapper/AdvancedMapperTest.java)

Тестовый модуль: [info.kgeorgiy.java.advanced.mapper](artifacts/info.kgeorgiy.java.advanced.mapper.jar)

Решение:
* [ParallelMapper](src/info/kgeorgiy/ja/olangaev/concurrent/ParallelMapperImpl.java)


## Домашнее задание 6. Итеративный параллелизм

Тестирование

 * простой вариант (`scalar`):
    * Класс должен реализовывать интерфейс
      [ScalarIP](modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent/ScalarIP.java).
    * [тесты](modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent/ScalarIPTest.java)
 * сложный вариант (`list`):
    * Класс должен реализовывать интерфейс
      [ListIP](modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent/ListIP.java).
    * [тесты](modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent/ListIPTest.java)
 * продвинутый вариант (`advanced`):
    * Класс должен реализовывать интерфейс
      [AdvancedIP](modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent/AdvancedIP.java).
    * [тесты](modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent/AdvancedIPTest.java)

Решение:
* [IterativeParallelism](src/info/kgeorgiy/ja/olangaev/concurrent/IterativeParallelism.java)

## Домашнее задание 4. Implementor

Класс `Implementor` должен реализовывать интерфейс
[Impler](modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/Impler.java).

Исходный код

 * простой вариант (`interface`): 
    [тесты](modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/InterfaceImplementorTest.java)
 * сложный вариант (`class`):
    [тесты](modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/ClassImplementorTest.java)
 * продвинутый вариант (`advanced`):
    [тесты](modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/AdvancedImplementorTest.java)
 * предварительные тесты бонусного варианта (`covariant`):
    [тесты](modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/AdvancedImplementorTest.java)

Решение:
* [Implementor](src/info/kgeorgiy/ja/olangaev/implementor/Implementor.java)
