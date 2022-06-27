/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
export default function isArray(input) {
    return input instanceof Array || Object.prototype.toString.call(input) === '[object Array]';
}
