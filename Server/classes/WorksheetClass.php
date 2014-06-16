<?php

class WorksheetClass {
	public static function post($contents) {

		// post it to the DB
		$db = DB::connect();
		$worksheetId = uniqid();
		$sql = "INSERT INTO worksheets (url, contents) VALUES (:url, :contents)";
		$insert = $db->prepare($sql);
		$insert->execute(array(":url"=>$worksheetId, ":contents"=>json_encode($contents)));

		echo $worksheetId;
	}

}

?>