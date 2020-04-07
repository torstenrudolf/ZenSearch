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

### Lookup by full field value
The algorithm holds the data per entity in a `Collection`; using `Array`s for fast constant time index lookup.
Additionally, it creates lookup `HashMap`s per entity for each field for all field values 
mapping to matching indices in the collection.

Because `HashMap` and `Array` lookups are of constant time, 
computational complexity of the search is of constant time or the order `O(1)`.

### Full text substring search on specified fields
When defining the `Entity`-set you can specify `searchableFields`.
The `FieldCache` will then create [`SuffixTree`s](https://en.wikipedia.org/wiki/Suffix_tree) for those fields.
SuffixTrees are a efficient way for substring searching.
To find all strings that contain the substring is only of linear order relative to the length of the substring.
But building the SuffixTrees has linear time-complexity relative to the number of strings.

I used the library [`gstlib`](https://github.com/GuillaumeDD/gstlib).
Unfortunately, this lib is currently not published for scala 2.13, so I forked it, 
ported it to 2.13 and added the jar under `/lib` to this project for now.
