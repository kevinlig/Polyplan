<?php

class RetrievalClass {

	public static function getItem($id) {

		$db = DB::connect();

		$sql = "SELECT contents FROM worksheets WHERE url = :id LIMIT 1";
		$query = $db->prepare($sql);
		$query->execute(array(":id"=>$id));

		$result = $query->fetch(PDO::FETCH_ASSOC);

		if ($query->rowCount() == 0) {
			// no results found
			
			echo json_encode(array("status"=>"error"));
			return;
		}


		echo json_encode(array("status"=>"success","data"=>json_decode($result['contents'])));

	}
}

?>