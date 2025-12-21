function gems:perf_stop
gems admin perf reset
gems admin setEnergy @a 10
gems admin resync @a
gems admin stress start @a 60 20 realistic true true

