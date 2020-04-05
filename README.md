# ZenSearch

This search app loads the entities (users, tickets and organizations) from json files into memory and 
allows you to search against them.


## Usage

In order to run the application simply run:
```
sbt run
```

Other commands:
* `sbt test` to run the tests
* `sbt docker` to build a docker image

If you don't want to build the project but simply run the application you can use this docker image:

```
docker run -it trudolf/zensearch
```


## The Algorithm
We assume that we can load all data into memory.

The algorithm holds the data per entity in a `Collection`; using `Array`s for fast constant time index lookup.
Additionally, it creates lookup `HashMap`s per entity for each field for all field values 
mapping to matching indices in the collection.

Because `HashMap` and `Array` lookups are of constant time, 
computational complexity of the search is of constant time or the order `O(1)`.




## Next Steps
Use suffix tries to do full text search.
