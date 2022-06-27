var restorePoints = [];
var imgIdUrlParam = -1;
$(document).ready(function(){

                  // This array will store the restoration points of the canvas
                  //var restorePoints = [];
                  var isMouseDown=false;
                  var b,c="";
                  var d=document.getElementById("can");
                  var ctx=d.getContext("2d");
                  var image = new Image();
                  ctx.strokeStyle="red";
                  ctx.lineWidth=5;
                  ctx.lineCap="round";
                  ctx.fillStyle="#fff";
                  ctx.fillRect(0,0,d.width,d.height);
                  image.onload = function() {
                     d.width = image.width;
                     d.height = image.height;
                     ctx.drawImage(image,0,0);
                     ctx.strokeStyle="red";
                     ctx.lineWidth=5;
                  }
                  var url = $.url();
                  var imgUrlParam = url.param('img');
                  imgIdUrlParam = url.param('imgid');
                  if(imgUrlParam) {
                     image.src = imgUrlParam;
                  } else if(imgIdUrlParam) {
                     image.src = "../loadImage.jsp?format=jpg&cotID="+imgIdUrlParam;
                     $.getJSON( "../GetCotData?cotId="+imgIdUrlParam, function(jsonResp) {
                        $("#uid").val(jsonResp.uid);
                        $("#how").val(jsonResp.how);
                        $("#type").val(jsonResp.type);
								if(jsonResp.lat) {
                        	$("#lat").val(jsonResp.lat);
								} else {
                        	$("#lat").val("");
								}
								if(jsonResp.lon) {
									$("#lon").val(jsonResp.lon);
								} else {
									$("#lon").val("");
								}
								if(jsonResp.hae) {
									$("#hae").val(jsonResp.hae);
								} else {
									$("#hae").val("");
								}
								if(jsonResp.le) {
									$("#le").val(jsonResp.le);
								} else {
									$("#le").val("");
								}
								if(jsonResp.ce) {
									$("#ce").val(jsonResp.ce);
								} else {
									$("#ce").val("");
								}
                        if(jsonResp.remarks) {
                           $("#remarks").val(jsonResp.remarks);
                        } else {
                           $("#remarks").val("");
                        }
                     });
                  }

                  $("#imageLoader").change(function(e){
                     var reader = new FileReader();
                     reader.onload = function(event){
                        var img = new Image();
                        img.onload = function(){
                           d.width = img.width;
                           d.height = img.height;
                           ctx.drawImage(img,0,0);
                        }
                        img.src = event.target.result;
                     }
                     reader.readAsDataURL(e.target.files[0]);     
                  });


                  $("#bsz").change(function(isMouseDown){ctx.lineWidth=this.value});
                  $("#bcl").change(function(isMouseDown){ctx.strokeStyle=this.value});
                  $("#can").mousedown(function(d){
                     saveRestorePoint();
                     isMouseDown=true;
                     ctx.save();
                     b=d.pageX-this.offsetLeft;
                     c=d.pageY-this.offsetTop
                  });


                  $(document).mouseup(function(){isMouseDown=false});
                  $(document).click(function(){isMouseDown=false});

                  $("#can").mousemove(function(d){
                                      if(isMouseDown==true){
                                      ctx.beginPath();
                                      ctx.moveTo(d.pageX-this.offsetLeft,d.pageY-this.offsetTop);
                                      ctx.lineTo(b,c);
                                      ctx.stroke();
                                      ctx.closePath();
                                      b=d.pageX-this.offsetLeft;
                                      c=d.pageY-this.offsetTop}}
                                     );

                  $("#clr > div").click(function(){
                                        ctx.strokeStyle=$(this).css("background-color");
                                        //$("#bcl").val($(this).css("background-color"))
                                        });
                  $("#undo").click(function(){undoDrawOnCanvas();});

                  $("#eraser").click(function(){ctx.strokeStyle="#fff"});
                  $("#save").click(function(){
                     //$("#result").html("<img src="+d.toDataURL()+' /><br /><a href="index.html?imgsrc='+d.toDataURL()+'" id="get" class="minimal" >Download This</a>');


                     $("#result").html("<img src="+d.toDataURL()+' /><br /><input type="button" value="Send Annotated Image" />');
                     $('input[type=button]').click(function(){
                        var dataURL = d.toDataURL("image/jpeg");
                        //var dataURL = d.getContext("2d").getImageData(0, 0, d.width, d.height);
                        dataURL = dataURL.replace(/^data:image\/(png|jpeg);base64,/, "");    
                        var dataObj = { "cotId" : imgIdUrlParam,
                                        "pic" : dataURL,
                                        "uid" : $("#uid").val(),
                                        "type" : $("#type").val(),
                                        "how" : $("#how").val()
													 };
                        if($("#lat").val().length > 0) {
                           dataObj.lat = $("#lat").val();
								}
                        if($("#lon").val().length > 0) {
                           dataObj.lon = $("#lon").val();
								}
                        if($("#hae").val().length > 0) {
                           dataObj.hae = $("#hae ").val();
								}
                        if($("#le").val().length > 0) {
                           dataObj.le = $("#le ").val();
								}
                        if($("#ce").val().length > 0) {
                           dataObj.ce = $("#ce ").val();
								}
                        if($("#remarks").val().length > 0) {
                           dataObj.remarks = $("#remarks").val();
                        }
                        var req = $.ajax({
                                          url: "../WriteToDb",
                                          type: "Post",
                                          data: dataObj,
                                          success: function(response)
                                          {
                                             window.location.href = "../gallery.jsp";
                                          }
                                         });
                     });
                     $("#data").val(d.toDataURL());
                     $("#get").click(function(){$("#frm").trigger("submit")})
                  });
                  $("#clear").click(function(){
                     ctx.fillStyle="#fff";
                     ctx.fillRect(0,0,d.width,d.height);
                     ctx.drawImage(image, 0, 0);
                     ctx.strokeStyle="red";
                     ctx.fillStyle="red"})
                  })


                  function saveRestorePoint() {
                     var oCanvas = document.getElementById("can");
                     var imgSrc = oCanvas.toDataURL("image/png");
                     restorePoints.push(imgSrc);
                  }

function undoDrawOnCanvas() {
   if (restorePoints.length > 0) {
      var oImg = new Image();
      oImg.onload = function() {
         var canvasContext = document.getElementById("can").getContext("2d");    
         canvasContext.drawImage(oImg, 0, 0);
      }
      oImg.src = restorePoints.pop();
   }
}

