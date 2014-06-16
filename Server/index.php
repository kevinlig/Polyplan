<?php
require 'vendor/autoload.php';
require("lib/database.php");

require("classes/WorksheetClass.php");
require("classes/RetrievalClass.php");

$app = new \Slim\Slim();
$app->get('/', function () use ($app) {
	$app->response->headers->set('X-Powered-By', 'Polyplan');
    echo "Hello world";
});

$app->get('/worksheets/:id', function($id) use ($app) {
	$app->response->headers->set('Content-Type', 'application/json');
	$app->response->headers->set('X-Powered-By', 'Polyplan');
	RetrievalClass::getItem($id);
});

$app->post('/worksheets', function() use ($app) {
	$app->response->headers->set('X-Powered-By', 'Polyplan');

	$data = json_decode($app->request->getBody(), true);

	$content = $data['data'];
	WorksheetClass::post($content);
});


$app->notFound(function() use ($app) {
	$app->response->headers->set('X-Powered-By', 'Polyplan');
	echo "Does not exist";
});



$app->run();



?>